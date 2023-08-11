#Usage: ./experiments/run.sh {subject_name} {gen_strategy} {mrs_to_fuzz} {allow_epa_loops}

. experiments/init_env.sh

subject_name=$1 
gen_strategy=$2
mrs_to_fuzz=$3
allow_epa_loops=$4
seed=$5

mkdir -p "output/$subject_name/allow_epa_loops_$allow_epa_loops/$gen_strategy/$mrs_to_fuzz/$seed/"

echo "Running $subject_name"
/usr/bin/time -o /dev/tty -f"%e" java -cp org.alloytools.alloy.application/target/org.alloytools.alloy.application.jar:org.alloytools.alloy.core/target/org.alloytools.alloy.core.jar:org.alloytools.alloy.dist/target/org.alloytools.alloy.dist.jar:org.alloytools.alloy.extra/target/org.alloytools.alloy.extra.jar:org.alloytools.alloy.lsp/target/org.alloytools.alloy.lsp.jar:org.alloytools.alloy.wrappers/target/org.alloytools.alloy.wrappers.jflex.jar:org.alloytools.alloy.wrappers/target/org.alloytools.alloy.wrappers.java_cup.jar:org.alloytools.api/target/org.alloytools.api.jar -Djava.library.path=solvers edu.mit.csail.sdg.alloy4whole.MRReducerCLI $subject_name $gen_strategy $mrs_to_fuzz $allow_epa_loops $seed > output/$subject_name/allow_epa_loops_$allow_epa_loops/$gen_strategy/$mrs_to_fuzz/$seed/log.txt 2> /dev/null
