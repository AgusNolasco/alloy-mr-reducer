/* Alloy Analyzer 4 -- Copyright (c) 2006-2009, Felix Chang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.mit.csail.sdg.alloy4whole;

import edu.mit.csail.sdg.alloy4.*;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public final class MRReducerCLI {

    private static String alloyHome = null;

    private static final String   fs = System.getProperty("file.separator");
    private static  String alloyHome() {
        if (alloyHome != null)
            return alloyHome;
        String temp = System.getProperty("java.io.tmpdir");
        if (temp == null || temp.length() == 0)
            OurDialog.fatal(null, "Error. JVM need to specify a temporary directory using java.io.tmpdir property.");
        String username = System.getProperty("user.name");
        File tempfile = new File(temp + File.separatorChar + "alloy4tmp40-" + (username == null ? "" : username));
        tempfile.mkdirs();
        String ans = Util.canon(tempfile.getPath());
        if (!tempfile.isDirectory()) {
            OurDialog.fatal(null,"Error. Cannot create the temporary directory " + ans);
        }
        if (!Util.onWindows()) {
            String[] args = {
                    "chmod", "700", ans
            };
            try {
                Runtime.getRuntime().exec(args).waitFor();
            } catch (Throwable ex) {
            } // We only intend to make a best effort.
        }
        return alloyHome = ans;
    }

    private static void copyFromJAR() {
        // Compute the appropriate platform
        String os = System.getProperty("os.name").toLowerCase(Locale.US).replace(' ', '-');
        if (os.startsWith("mac-"))
            os = "mac";
        else if (os.startsWith("windows-"))
            os = "windows";
        String arch = System.getProperty("os.arch").toLowerCase(Locale.US).replace(' ', '-');
        if (arch.equals("powerpc"))
            arch = "ppc-" + os;
        else
            arch = arch.replaceAll("\\Ai[3456]86\\z", "x86") + "-" + os;
        if (os.equals("mac"))
            arch = "x86-mac"; // our pre-compiled binaries are all universal
        // binaries
        // Find out the appropriate Alloy directory
        final String platformBinary = alloyHome() + fs + "binary";
        // Write a few test files
        try {
            (new File(platformBinary)).mkdirs();
            Util.writeAll(platformBinary + fs + "tmp.cnf", "p cnf 3 1\n1 0\n");
        } catch (Err er) {
            // The error will be caught later by the "berkmin" or "spear" test
        }
        // Copy the platform-dependent binaries
        Util.copy(null, true, false, platformBinary, arch + "/libminisat.so", arch + "/libminisatx1.so", arch + "/libminisat.jnilib", arch + "/libminisat.dylib", arch + "/libminisatprover.so", arch + "/libminisatproverx1.so", arch + "/libminisatprover.jnilib", arch + "/libminisatprover.dylib", arch + "/libzchaff.so", arch + "/libzchaffmincost.so", arch + "/libzchaffx1.so", arch + "/libzchaff.jnilib", arch + "/liblingeling.so", arch + "/liblingeling.dylib", arch + "/liblingeling.jnilib", arch + "/plingeling", arch + "/libglucose.so", arch + "/libglucose.dylib", arch + "/libglucose.jnilib", arch + "/libcryptominisat.so", arch + "/libcryptominisat.la", arch + "/libcryptominisat.dylib", arch + "/libcryptominisat.jnilib", arch + "/berkmin", arch + "/spear", arch + "/cryptominisat");
        Util.copy(null, false, false, platformBinary, arch + "/minisat.dll", arch + "/cygminisat.dll", arch + "/libminisat.dll.a", arch + "/minisatprover.dll", arch + "/cygminisatprover.dll", arch + "/libminisatprover.dll.a", arch + "/glucose.dll", arch + "/cygglucose.dll", arch + "/libglucose.dll.a", arch + "/zchaff.dll", arch + "/berkmin.exe", arch + "/spear.exe");
        // Record the locations
        System.setProperty("alloy.home", alloyHome());

    }

    private static boolean _loadLibrary(String library) {
        try {
            System.loadLibrary(library);
            return true;
        } catch (UnsatisfiedLinkError ex) {}
        try {
            System.loadLibrary(library + "x1");
            return true;
        } catch (UnsatisfiedLinkError ex) {}
        try {
            System.loadLibrary(library + "x2");
            return true;
        } catch (UnsatisfiedLinkError ex) {}
        try {
            System.loadLibrary(library + "x3");
            return true;
        } catch (UnsatisfiedLinkError ex) {}
        try {
            System.loadLibrary(library + "x4");
            return true;
        } catch (UnsatisfiedLinkError ex) {}
        try {
            System.loadLibrary(library + "x5");
            return true;
        } catch (UnsatisfiedLinkError ex) {
            return false;
        }
    }

    private static boolean loadLibrary(String library) {
        boolean loaded = _loadLibrary(library);
        String libName = System.mapLibraryName(library);
        if (loaded)
            System.out.println("Loaded: " + libName);
        else
            System.out.println("Failed to load: " + libName);
        return loaded;
    }

    public static void main(String[] args) throws Exception {
        copyFromJAR();
        final String binary = alloyHome() + fs + "binary";
        try {
            System.setProperty("java.library.path", binary);
            String[] newarray = new String[] {
                    binary
            };
            java.lang.reflect.Field old = ClassLoader.class.getDeclaredField("usr_paths");
            old.setAccessible(true);
            old.set(null, newarray);
        } catch (Throwable ignored) {
        }
        loadLibrary("minisat");
        A4Reporter reporter = new A4Reporter();
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.MiniSatJNI;
        options.solverDirectory = binary;

        String mrInferenceDir = Optional.ofNullable(System.getenv("MR_INFERENCE_DIR")).orElseThrow(
                () -> new IllegalStateException("MR_INFERENCE_DIR is not set in the environment"));
        String randoopDir = Optional.ofNullable(System.getenv("RANDOOP_DIR")).orElseThrow(
                () -> new IllegalStateException("RANDOOP_DIR is not set in the environment"));

        String clazz = args[0];
        String gen_strategy = args[1];
        String mrs_to_fuzz = args[2];
        String allow_epa_loops = args[3];
        String EPAAlloyModelDir = mrInferenceDir + "/output/" + clazz + "/" + "allow_epa_loops_" + allow_epa_loops + "/";
        String MRsPredModelsDir = randoopDir + "/output/" + clazz + "/" + "allow_epa_loops_" + allow_epa_loops + "/" + gen_strategy + "/" + mrs_to_fuzz + "/";

        if (Files.notExists(Paths.get(EPAAlloyModelDir))) {
            throw new IllegalArgumentException("The path: " + EPAAlloyModelDir + " does not exist");
        }
        if (Files.notExists(Paths.get(MRsPredModelsDir))) {
            throw new IllegalArgumentException("The path: " + MRsPredModelsDir + " does not exist");
        }

        File file = new File(EPAAlloyModelDir + "epa-alloy-model.als");
        FileReader reader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String EPAModel = bufferedReader.lines().collect(Collectors.joining("\n"));

        file = new File(MRsPredModelsDir + "mrs-alloy-predicates.als");
        reader = new FileReader(file);
        bufferedReader = new BufferedReader(reader);

        List<String> mrs = new ArrayList<>();
        Map<String, String> mrToPredicate = new HashMap<>();
        Map<String, String> mrToFormattedMr = new HashMap<>();
        for (String line : bufferedReader.lines().collect(Collectors.toSet())) {
            String[] data = line.split(" # ");
            mrs.add(data[0]);
            mrToPredicate.put(data[0], data[1]);
            mrToFormattedMr.put(data[0], data[2]);
        }

        mrs.sort(mrsComparator);

        {
            int i = 0;
            for (String mr : mrs) {
                System.out.println(++i + " - " + mr);
            }
        }

        List<String> modelLines = Arrays.asList(EPAModel.split("\n"));
        List<String> epaStates = modelLines.subList(modelLines.indexOf("fact{") + 1, modelLines.size());
        epaStates = epaStates.subList(0, epaStates.indexOf("}"));

        System.out.println("EPA STATES NUM:" + epaStates.size());
        epaStates = epaStates.stream().map(s -> s.replaceAll("some", "").replaceAll(" ", "").replaceAll("\t", "")).collect(Collectors.toList());

        Set<String> impliedMRs = new HashSet<>();
        for (int i = 0; i < mrs.size(); i++) {
            String mr = mrs.get(i);
            String otherMRsPredicates = mrs.subList(i + 1, mrs.size()).stream()
                    .map(mr2 -> "(" + mrToPredicate.get(mr2) + ")").collect(Collectors.joining(" and "));
            String toCheck = EPAModel +
                    "\npred MR[] { " + mrToPredicate.get(mr) + " }" +
                    "\npred OthersMRs[] { " + otherMRsPredicates + " }" +
                    "\nassert MRIsImplied { OthersMRs[] implies MR[] }\n" +
                    "check MRIsImplied for 0 but " + epaStates.stream().map(s -> "3 " + s).collect(Collectors.joining(", "));
            System.out.println(toCheck);
            System.out.println(i+1);
            toCheck = toCheck.replaceAll("set", "sett");
            CompModule world = CompUtil.parseEverything_fromString(reporter, toCheck);
            ConstList<Command> commands = world.getAllCommands();
            assert commands.size() == 1;
            boolean result;
            Command c = commands.get(0);
            A4Solution sol;
            sol = TranslateAlloyToKodkod.execute_command(null, world.getAllReachableSigs(), c, options);
            result = !sol.satisfiable(); // Alloy checks satisfiability over the negation of the predicate
            System.out.println("MR: " + mr);
            if (result) {
                System.out.println("The MR is implicated by the others");
                impliedMRs.add(mr);
            } else {
                System.out.println("There's no implication");
            }
            System.out.println("********************");
        }

        Set<String> reducedSetOfMRs = new HashSet<>(mrs);
        reducedSetOfMRs.removeAll(impliedMRs);

        System.out.println(String.join("\n", reducedSetOfMRs));

        Set<String> reducedSetOfFormattedMrs = new HashSet<>();
        for (String mr : reducedSetOfMRs) {
            reducedSetOfFormattedMrs.add(mrToFormattedMr.get(mr));
        }

        String outputDir = "output/" + clazz + "/" + "allow_epa_loops_" + allow_epa_loops + "/" + gen_strategy + "/" + mrs_to_fuzz + "/";
        saveResults(outputDir, reducedSetOfMRs, impliedMRs);
        saveFormattedMrs(outputDir, reducedSetOfFormattedMrs);
    }

    private static void saveFormattedMrs(String outputDir, Set<String> reducedSetOfFormattedMrs) {
        File directory = new File(outputDir);
        if (!directory.exists()){
            directory.mkdirs();
        }

        try (FileWriter writer = new FileWriter(outputDir + "formatted-mrs.csv")) {
            writer.write(String.join("\n", reducedSetOfFormattedMrs));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveResults(String outputDir, Set<String> reducedSetOfMRs, Set<String> impliedMRs) {
        File directory = new File(outputDir);
        if (!directory.exists()){
            directory.mkdirs();
        }

        try (FileWriter writer = new FileWriter(outputDir + "mrs.txt")) {
            writer.write("Reduced Set of MRs: \n\n");
            writer.write(String.join("\n", reducedSetOfMRs));
            writer.write("\n");
            writer.write("\nImplied MRs: \n\n");
            writer.write(String.join("\n", impliedMRs));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Comparator<String> mrsComparator = (mr1, mr2) -> {
        String prop1 = mr1.split(" -> ")[1];
        String prop2 = mr2.split(" -> ")[1];
        String leftPart1 = prop1.split(" = ")[0];
        String rightPart1 = prop1.split(" = ")[1];
        String leftPart2 = prop2.split(" = ")[0];
        String rightPart2 = prop2.split(" = ")[1];

        int mr1Size = 0;
        int mr2Size = 0;

        if (!leftPart1.equals("λ")) {
            mr1Size += leftPart1.split(" ").length;
        }
        if (!leftPart2.equals("λ")) {
            mr2Size += leftPart2.split(" ").length;
        }
        mr1Size += rightPart1.split(" ").length;
        mr2Size += rightPart2.split(" ").length;

        return mr2Size - mr1Size;
    };

}
