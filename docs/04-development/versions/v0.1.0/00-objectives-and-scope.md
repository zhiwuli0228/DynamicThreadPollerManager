# v0.1.0 Objectives and Scope

## Header

- Version name: `v0.1.0`
- Document purpose: define the problem statement, goals, and version boundary
- Status: `DRAFT`

## 1. Objectives

The first version exists to establish a repeatable research environment for dynamic thread-pool control.

The project should be able to answer:

- whether adaptive scaling improves behavior under tide-like load;
- which runtime signals are worth using as control inputs;
- how thread scaling and queue scaling affect each other;
- how much oscillation is introduced by different control rules;
- which strategy is the best baseline for future experiments.

## 2. Scope

In scope:

- load scenario generation
- pressure snapshot sampling
- policy evaluation
- executor adjustment
- experiment record generation
- repeatable comparison across strategies

Out of scope:

- production rollout
- polished dashboard
- external orchestration integration
- model training and online ML inference
- OpenSpec change decomposition authorization

## 3. Primary value

The first version should create a trusted experimental baseline, not a feature-rich framework.

The design is successful if later work can reuse the same experimental language, scenario definitions, and metrics without rewriting the core assumptions.
