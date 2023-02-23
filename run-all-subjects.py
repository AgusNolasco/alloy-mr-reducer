import os
import subprocess
import sys

subject_set  = sys.argv[1]
gen_strategy = sys.argv[2]
mrs_to_fuzz  = sys.argv[3]
allow_epa_loops = sys.argv[4]

subjects_path = f'experiments/{subject_set}-subjects.txt'
with open(subjects_path) as subjects_file:
    for subject in subjects_file:
        result = subprocess.run(f'/usr/bin/time -f"%e" experiments/run.sh {subject} {gen_strategy} {mrs_to_fuzz} {allow_epa_loops}', shell=True)
