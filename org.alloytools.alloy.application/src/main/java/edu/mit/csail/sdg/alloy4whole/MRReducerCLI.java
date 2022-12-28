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
    private static String OUTPUT_DIR;

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
        } catch (Throwable ex) {
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
        OUTPUT_DIR = "output/" + clazz + "/";
        String EPAAlloyModelDir = mrInferenceDir + "/" + OUTPUT_DIR;
        String MRsPredModelsDir = randoopDir + "/" + OUTPUT_DIR;

        if (Files.notExists(Paths.get(EPAAlloyModelDir))) {
            throw new IllegalArgumentException("The path: " + EPAAlloyModelDir + " does not exist");
        }
        if (Files.notExists(Paths.get(MRsPredModelsDir))) {
            throw new IllegalArgumentException("The path: " + MRsPredModelsDir + " does not exist");
        }

        File file = new File(EPAAlloyModelDir + "EPA_alloy_model.als");
        FileReader reader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String EPAModel = bufferedReader.lines().collect(Collectors.joining("\n"));

        file = new File(MRsPredModelsDir + "MRs_alloy_predicates.als");
        reader = new FileReader(file);
        bufferedReader = new BufferedReader(reader);

        Map<String, String> mrToPredicate = new HashMap<>();
        for (String line : bufferedReader.lines().collect(Collectors.toSet())) {
            String[] mrAndPred = line.split(" # ");
            mrToPredicate.put(mrAndPred[0], mrAndPred[1]);
        }

        Set<String> impliedMRs = new HashSet<>();
        for (String mr1 : mrToPredicate.keySet()) {
            for (String mr2 : mrToPredicate.keySet()) {
                if (!mr1.equals(mr2)) {
                    String toCheck = EPAModel +
                            "\npred MR1[] { " + mrToPredicate.get(mr1) + " }" +
                            "\npred MR2[] { " + mrToPredicate.get(mr2) + " }" +
                            "\nassert MR1ImpliesMR2 { MR1[] implies MR2[] }\n" +
                            "check MR1ImpliesMR2 for 10\n" +
                            "assert MR2ImpliesMR1 { MR2[] implies MR1[] }\n" +
                            "check MR2ImpliesMR1 for 10";
                    CompModule world = CompUtil.parseEverything_fromString(reporter, toCheck);
                    ConstList<Command> commands = world.getAllCommands();
                    assert commands.size() == 2;
                    boolean[] results = new boolean[2];
                    for (int i = 0; i < commands.size(); i++) {
                        Command c = commands.get(i);
                        A4Solution sol;
                        sol = TranslateAlloyToKodkod.execute_command(null, world.getAllReachableSigs(), c, options);
                        results[i] = !sol.satisfiable(); // Alloy checks satisfiability over the negation of the predicate
                    }
                    System.out.println("MR1: " + mr1);
                    System.out.println("MR2: " + mr2);
                    if (results[0] && results[1]) {
                        System.out.println("MR1 is equivalent to MR2");
                        impliedMRs.add(mr1.length() > mr2.length() ? mr1 : mr2);
                    } else if (results[0]) {
                        System.out.println("MR1 implies MR2");
                        impliedMRs.add(mr2);
                    } else if (results[1]) {
                        System.out.println("MR2 implies MR1");
                        impliedMRs.add(mr1);
                    } else {
                        System.out.println("There's no any implication");
                    }
                    System.out.println("********************");
                }
            }
        }

        Set<String> reducedSetOfMRs = new HashSet<>(mrToPredicate.keySet());
        reducedSetOfMRs.removeAll(impliedMRs);

        saveResults(reducedSetOfMRs, impliedMRs);
    }

    private static void saveResults(Set<String> reducedSetOfMRs, Set<String> impliedMRs) {
        File directory = new File(OUTPUT_DIR);
        if (!directory.exists()){
            directory.mkdirs();
        }

        try (FileWriter writer = new FileWriter(OUTPUT_DIR + "reduction.txt")) {
            writer.write("Reduced Set of MRs: \n\n");
            writer.write(String.join("\n", reducedSetOfMRs));
            writer.write("\n");
            writer.write("\nImplied MRs: \n\n");
            writer.write(String.join("\n", impliedMRs));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
