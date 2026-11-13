# Twist validation plan

## Principles

- **Session-level** labels only.  
- **Leave-One-Session-Out (LOSO)** or leave-one-hold-out — never random sample splits within a gesture.  
- Report FP rate on negative sessions and continuous “daily motion” captures when available.  
- Do not treat Mot G42 scores as F7 ground truth.

## Metrics

| Metric | Definition |
|--------|------------|
| Precision | TP / (TP+FP) at session or event level (state clearly) |
| Recall | TP / (TP+FN) on positive sessions |
| F1 | Harmonic mean |
| FN list | Missed legitimate double twists |
| FP / hour | From long negative / idle / walk recordings (when collected) |

## Classifier candidates (offline first)

1. Current threshold detector (baseline replay on new CSVs).  
2. Multivariate rules (p2p + interpeak + integral + zc).  
3. **Temporal state machine:** IDLE → ROT1 → REVERSE → ROT2 → ACCEPT / TIMEOUT reject.  
4. Peak-sequence detector (signed peaks + similarity).  
5. DTW to Mot/F7 templates (if n sufficient).  
6. Light ML only if session count justifies (still LOSO).

## Regression

Any detector change must re-run Chop unit tests and a short Chop smoke on device before a future release candidate. **No public release until evidence shows clear Twist improvement.**

## Status

Validation tables will be filled after the new F7 capture campaign. Placeholder: `experiments/twist/FEATURE_TABLE.csv`.
