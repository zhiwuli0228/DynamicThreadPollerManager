package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcquisitionReportPathsExtensionTest {

    @Test
    void shouldGenerateEvidenceFileName() {
        String name = AcquisitionReportPaths.evidenceFileName("run-001");
        assertEquals("run-001-evidence.jsonl", name);
    }

    @Test
    void shouldGenerateSessionMetadataFileName() {
        String name = AcquisitionReportPaths.sessionMetadataFileName("run-001");
        assertEquals("run-001-session.json", name);
    }

    @Test
    void shouldGenerateEvidenceFilePath() {
        Path root = Path.of("/tmp");
        Path path = AcquisitionReportPaths.evidenceFile(root, "run-001");
        assertTrue(path.toString().replace('\\', '/').endsWith(
                "outputs/reports/v0.11.0/run-001-evidence.jsonl"));
    }

    @Test
    void shouldGenerateSessionMetadataFilePath() {
        Path root = Path.of("/tmp");
        Path path = AcquisitionReportPaths.sessionMetadataFile(root, "run-001");
        assertTrue(path.toString().replace('\\', '/').endsWith(
                "outputs/reports/v0.11.0/run-001-session.json"));
    }

    @Test
    void shouldRejectNullRunId() {
        try {
            AcquisitionReportPaths.evidenceFileName(null);
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    void shouldRejectBlankRunId() {
        try {
            AcquisitionReportPaths.evidenceFileName("");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
