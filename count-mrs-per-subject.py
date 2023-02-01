import os
import sys

gen_strategy = sys.argv[1]
mrs_to_fuzz  = sys.argv[2]
allow_epa_loops = sys.argv[3]

def split_mr(mr):
    precond = mr.split(' -> ')[0]
    precond = precond[1:len(precond)-1]
    preconds = precond.split(', ')
    mrs = []
    for cond in preconds:
        mrs.append(f'[{cond}] -> {mr.split(" -> ")[1]}')
    return mrs

def split_mrs(mrs):
    split_mrs = []
    for mr in mrs:
        split_mrs.extend(split_mr(mr))
    return split_mrs

for subject in os.listdir('output'):
    path_to_mrs = f'output/{subject}/allow_epa_loops_{allow_epa_loops}/{gen_strategy}/{mrs_to_fuzz}/mrs.txt'
    with open(path_to_mrs) as mrs_file:
        mrs = [line.rstrip() for line in mrs_file]
        first_mark = mrs.index('')
        mrs = mrs[first_mark+1:]
        second_mark = mrs.index('')
        mrs = mrs[:second_mark]
        mrs = split_mrs(mrs)
        print(f'{subject}: {len(mrs)}')
