<#
.SYNOPSIS
    Archive guard for OpenSpec changes. Hard gate that catches real
    problems before an archive is honored, so future agents do not
    silently re-introduce the residual inconsistencies this script
    was written to detect.

.DESCRIPTION
    Two modes:

      pre-finalize   Runs BEFORE /opsx:finalize, on an active change.
                     Hard-checks:
                       * openspec validate --all --json is green
                       * active change directory exists
                       * current-state still authorizes
                         EXECUTION_AUTHORIZED and the supplied change
                         (either 'Change name: <name>' or
                         'Authorized OpenSpec change: <name>')
                       * openspec list --json still references the
                         change

      post-archive   Runs AFTER /opsx:archive. Hard-checks every
                     state required for archive completion — none of
                     these are warnings, all are hard gates:
                       * openspec validate --all --json is green
                       * active change directory is GONE
                       * archive directory exists
                       * main spec exists at
                         openspec/specs/<name>/spec.md and contains
                         both `## Purpose` and `## Requirements`
                       * current-state no longer lists the change as
                         active (neither 'Change name: <name>' nor
                         'Authorized OpenSpec change: <name>' may
                         remain) and no longer declares
                         Current stage: EXECUTION_AUTHORIZED
                       * openspec list --json no longer references
                         the change
                       * `git status --short` is empty (worktree
                         clean) — this is the default and the only
                         behavior. There is no lenient mode, no
                         -RequireClean switch, and no way to bypass
                         the cleanliness check from this script.

    Exit codes:
      0  all hard checks passed
      1  at least one hard check failed (reason on stderr)

.PARAMETER Mode
    pre-finalize | post-archive

.PARAMETER ChangeName
    The change name (e.g. offline-replay-and-readiness-gate).

.PARAMETER ArchiveDirName
    Optional. The exact archive directory name to inspect
    (post-archive mode only). If omitted, the most recent archive
    directory matching *-<ChangeName> is used.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/openspec-archive-guard.ps1 `
        -Mode pre-finalize -ChangeName offline-replay-and-readiness-gate

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/openspec-archive-guard.ps1 `
        -Mode post-archive -ChangeName offline-replay-and-readiness-gate
#>

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("pre-finalize", "post-archive")]
    [string]$Mode,

    [Parameter(Mandatory = $true)]
    [string]$ChangeName,

    [string]$ArchiveDirName
)

$ErrorActionPreference = "Stop"

function Fail([string]$Message) {
    Write-Error $Message
    exit 1
}

function Require-Path([string]$Path, [string]$Message) {
    if (-not (Test-Path $Path)) {
        Fail $Message
    }
}

function Require-TextMatch([string]$Text, [string]$Pattern, [string]$Message) {
    if ($Text -notmatch $Pattern) {
        Fail $Message
    }
}

function Require-TextNoMatch([string]$Text, [string]$Pattern, [string]$Message) {
    if ($Text -match $Pattern) {
        Fail $Message
    }
}

function Get-ValidateFailures {
    # Returns the number of items that failed validation. Reads the
    # JSON summary rather than relying on the CLI exit code, so a
    # non-zero exit is captured even if the CLI changes its exit
    # contract in the future.
    $output = & openspec.cmd validate --all --json 2>&1
    $jsonText = ($output -join "`n")
    try {
        $parsed = $jsonText | ConvertFrom-Json -ErrorAction Stop
    } catch {
        Fail "openspec validate output was not parseable JSON.`n$jsonText"
    }
    if ($null -ne $parsed.summary -and $null -ne $parsed.summary.totals) {
        return [int]$parsed.summary.totals.failed
    }
    # No summary; fall back to a scan of all items.
    $failed = 0
    foreach ($item in $parsed) {
        if ($item.PSObject.Properties['valid'] -and -not $item.valid) {
            $failed++
        }
    }
    return $failed
}

function Get-OpenSpecListMentions {
    $output = & openspec.cmd list --json 2>&1
    $jsonText = ($output -join "`n")
    try {
        $parsed = $jsonText | ConvertFrom-Json -ErrorAction Stop
    } catch {
        Fail "openspec list output was not parseable JSON.`n$jsonText"
    }
    $names = @()
    if ($parsed.PSObject.Properties['changes']) {
        foreach ($change in $parsed.changes) {
            $names += [string]$change.name
        }
    }
    return ,$names
}

function Get-WorktreeStatus {
    $status = (& git status --short)
    if ($null -eq $status) { return @() }
    return @($status | Where-Object { $_ -ne "" })
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$currentStatePath = Join-Path $repoRoot "docs/00-project/current-state.md"
Require-Path $currentStatePath "Missing current-state authority file: $currentStatePath"
$currentState = Get-Content -Raw $currentStatePath

$validateFailures = Get-ValidateFailures
if ($validateFailures -gt 0) {
    Fail "openspec validate reports $validateFailures failed item(s); see output above."
}

$worktreeEntries = Get-WorktreeStatus

# current-state.md has used two interchangeable labels for the active
# change: "Change name: <name>" (legacy, used through the offline-replay
# authorization) and "Authorized OpenSpec change: <name>" (current,
# introduced when the executor-adapter change was authorized). They are
# semantically equivalent; the guard accepts either so the contract does
# not silently break on every authorize commit.
#
# The backticks are written inside a single-quoted here-string so the
# regex engine receives literal backticks; PowerShell's double-quoted
# strings would silently drop the unescaped `` ` `` characters and
# break the pattern.
$activeChangePattern = @'
(?:Change name|Authorized OpenSpec change):\s+`?
'@ + [regex]::Escape($ChangeName) + @'
`?
'@

switch ($Mode) {
    "pre-finalize" {
        $activeChangePath = Join-Path $repoRoot ("openspec/changes/" + $ChangeName)
        Require-Path $activeChangePath "Active change path not found: $activeChangePath"
        Require-TextMatch $currentState 'Current stage:\s+`?EXECUTION_AUTHORIZED`?' `
            "current-state is not in EXECUTION_AUTHORIZED while pre-finalize guard expects active execution."
        Require-TextMatch $currentState $activeChangePattern `
            "current-state does not identify the active change $ChangeName (expected 'Change name: <name>' or 'Authorized OpenSpec change: <name>')."

        $activeChangeNames = Get-OpenSpecListMentions
        if ($activeChangeNames -notcontains $ChangeName) {
            Fail "openspec list does not contain the active change $ChangeName; pre-finalize state is incoherent.`n$($activeChangeNames -join ', ')"
        }

        Write-Output "PASS pre-finalize: validate green, active change exists, current-state authorizes $ChangeName, list agrees."
    }
    "post-archive" {
        $activeChangePath = Join-Path $repoRoot ("openspec/changes/" + $ChangeName)
        if (Test-Path $activeChangePath) {
            Fail "Active change path still exists after archive: $activeChangePath"
        }

        if (-not $ArchiveDirName) {
            $archiveDirs = Get-ChildItem (Join-Path $repoRoot "openspec/changes/archive") -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -like "*-$ChangeName" } |
                Sort-Object Name -Descending
            if (-not $archiveDirs) {
                Fail "No archive directory found for change $ChangeName under openspec/changes/archive/."
            }
            $archiveDir = $archiveDirs | Select-Object -First 1
        } else {
            $archiveDir = Get-Item (Join-Path $repoRoot ("openspec/changes/archive/" + $ArchiveDirName))
        }

        $mainSpecPath = Join-Path $repoRoot ("openspec/specs/" + $ChangeName + "/spec.md")
        Require-Path $mainSpecPath "Synced main spec not found: $mainSpecPath"
        $mainSpec = Get-Content -Raw $mainSpecPath
        Require-TextMatch $mainSpec '(?m)^## Purpose\s*$' "Main spec is missing '## Purpose': $mainSpecPath"
        Require-TextMatch $mainSpec '(?m)^## Requirements\s*$' "Main spec is missing '## Requirements': $mainSpecPath"

        Require-TextNoMatch $currentState $activeChangePattern `
            "current-state still lists archived change $ChangeName as active (expected neither 'Change name: <name>' nor 'Authorized OpenSpec change: <name>' to be present)."
        Require-TextNoMatch $currentState 'Current stage:\s+`?EXECUTION_AUTHORIZED`?' `
            "current-state still shows EXECUTION_AUTHORIZED after archive."

        $listNames = Get-OpenSpecListMentions
        if ($listNames -contains $ChangeName) {
            Fail "openspec list still references archived change $ChangeName.`n$($listNames -join ', ')"
        }

        # Clean worktree is a hard gate in post-archive mode. There is
        # no lenient mode and no -RequireClean switch: a non-empty
        # `git status --short` always fails. This catches the most
        # common archive-residual bug — implementation, archive
        # artifacts, and governance edits all sitting uncommitted in
        # the working tree.
        if ($worktreeEntries.Count -gt 0) {
            $joined = ($worktreeEntries -join "`n")
            Fail ("Worktree is not clean after archive ($($worktreeEntries.Count) entries).`n" +
                  "Commit or stash every entry below before claiming archive is complete; " +
                  "this script has no lenient mode for this case.`n" +
                  $joined)
        }

        Write-Output ("PASS post-archive: validate green, archive dir present ({0}), main spec valid, current-state synchronized, list synchronized, worktree clean." -f $archiveDir.Name)
    }
}
