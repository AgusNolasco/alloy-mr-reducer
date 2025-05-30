import os
import subprocess
import sys

subject_set  = sys.argv[1]
gen_strategy = sys.argv[2]
mrs_to_fuzz  = sys.argv[3]
allow_epa_loops = sys.argv[4]

seeds = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]

subjects_path = f'experiments/{subject_set}-subjects.txt'
with open(subjects_path) as subjects_file:
    for subject in subjects_file:
        subject = subject.strip()
        if '#' in subject:
            continue
        for seed in seeds:
            result = subprocess.run(f'experiments/run.sh {subject} {gen_strategy} {mrs_to_fuzz} {allow_epa_loops} {seed}', shell=True)
