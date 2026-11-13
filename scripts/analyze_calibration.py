#!/usr/bin/env python3
"""Analyze F7 Moto Actions calibration CSV and suggest thresholds."""
from __future__ import annotations

import csv
import math
import sys
from pathlib import Path


def main(path: str) -> None:
    g = [0.0, 0.0, 9.8]
    init = False
    chop, twist, walk = [], [], []
    with open(path, newline="") as f:
        for row in csv.DictReader(f):
            ax, ay, az = float(row["ax"]), float(row["ay"]), float(row["az"])
            gx, gy, gz = float(row["gx"]), float(row["gy"]), float(row["gz"])
            label = row["label"]
            if not init:
                g = [ax, ay, az]
                init = True
            a = 0.9
            g = [a * g[i] + (1 - a) * v for i, v in enumerate((ax, ay, az))]
            dyn = math.sqrt(sum((v - g[i]) ** 2 for i, v in enumerate((ax, ay, az))))
            rate = max(abs(gy), abs(gz))
            if label == "chop":
                chop.append(dyn)
            elif label == "twist":
                twist.append(rate)
            elif label == "walk":
                walk.append(dyn)

    def pct(xs, p):
        if not xs:
            return None
        xs = sorted(xs)
        return xs[min(len(xs) - 1, int(len(xs) * p / 100))]

    chop_s = pct([x for x in chop if x > 5], 70)
    twist_s = pct([x for x in twist if x > 2], 70)
    walk_max = max(walk) if walk else None
    print(f"samples chop={len(chop)} twist={len(twist)} walk={len(walk)}")
    print(f"suggested chop_peak={None if chop_s is None else round(chop_s * 0.85, 2)}")
    print(f"suggested twist_peak={None if twist_s is None else round(twist_s * 0.85, 2)}")
    print(f"walk_max_dyn={None if walk_max is None else round(walk_max, 2)}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(f"usage: {Path(sys.argv[0]).name} samples.csv")
        sys.exit(1)
    main(sys.argv[1])
