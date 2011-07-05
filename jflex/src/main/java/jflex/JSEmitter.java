/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jflex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JavaScript emitter 
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class JSEmitter implements IEmitter {

    // bit masks for state attributes
    static final private int FINAL = 1;

    static final private int NOLOOK = 8;

    static final private String date = (new SimpleDateFormat()).format(new Date());

    private File inputFile;

    private PrintWriter out;

    private Skeleton skel;

    private LexScan scanner;

    private LexParse parser;

    private DFA dfa;

    // for switch statement:
    // table[i][j] is the set of input characters that leads from state i to state j
    private CharSet table[][];

    private boolean isTransition[];

    // noTarget[i] is the set of input characters that have no target state in state i
    private CharSet noTarget[];

    // for row killing:
    private int numRows;

    private int[] rowMap;

    private boolean[] rowKilled;

    // for col killing:
    private int numCols;

    private int[] colMap;

    private boolean[] colKilled;

    /** maps actions to their switch label */
    private Map<Action, Integer> actionTable = new HashMap<Action, Integer>();

    private CharClassInterval[] intervals;

    public JSEmitter(File inputFile, LexParse parser, DFA dfa) throws IOException {

        String name = getBaseName(parser.scanner.className) + ".js";

        File outputFile = normalize(name, inputFile);

        Out.println("Writing code to \"" + outputFile + "\"");

        this.out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
        this.parser = parser;
        this.scanner = parser.scanner;
        this.inputFile = inputFile;
        this.dfa = dfa;
        this.skel = new Skeleton(out, null);
        this.skel.setSize(21);
        this.skel.readResource("jflex/skeleton-js.default");
    }

    /**
     * Computes base name of the class name. Needs to take into account generics.
     *
     * @param className Class name for which to construct the base name
     * @see LexScan#className
     * @return the
     */
    public static String getBaseName(String className) {
        int gen = className.indexOf('<');
        if (gen < 0) {
            return className;
        } else {
            return className.substring(0, gen);
        }
    }

    /**
     * Constructs a file in Options.getDir() or in the same directory as
     * another file. Makes a backup if the file already exists.
     *
     * @param name  the name (without path) of the file
     * @param input fall back location if path = <tt>null</tt>
     *              (expected to be a file in the directory to write to)   
     * @return The constructed File
     */
    public static File normalize(String name, File input) {
        File outputFile;

        if (Options.getDir() == null) {
            if (input == null || input.getParent() == null) {
                outputFile = new File(name);
            } else {
                outputFile = new File(input.getParent(), name);
            }
        } else {
            outputFile = new File(Options.getDir(), name);
        }

        if (outputFile.exists() && !Options.no_backup) {
            File backup = new File(outputFile.toString() + "~");

            if (backup.exists()) {
                backup.delete();
            }

            if (outputFile.renameTo(backup)) {
                Out.println("Old file \"" + outputFile + "\" saved as \"" + backup + "\"");
            } else {
                Out.println("Couldn't save old file \"" + outputFile + "\", overwriting!");
            }
        }

        return outputFile;
    }

    private void println() {
        out.println();
    }

    private void println(String line) {
        out.println(line);
    }

    private void println(int i) {
        out.println(i);
    }

    private void print(String line) {
        out.print(line);
    }

    private void print(int i) {
        out.print(i);
    }

    private void print(int i, int tab) {
        int exp;

        if (i < 0) {
            exp = 1;
        } else {
            exp = 10;
        }

        while (tab-- > 1) {
            if (Math.abs(i) < exp) {
                print(" ");
            }
            exp *= 10;
        }

        print(i);
    }

    private boolean hasGenLookAhead() {
        return dfa.lookaheadUsed;
    }

    private void emitLookBuffer() {
        if (!hasGenLookAhead()) {
            return;
        }

        println("/** For the backwards DFA of general lookahead statements */");
        println("var zzFin = [];");
        println();
    }

    private void emitScanError() {
        print("  const zzScanError = function(errorCode)");
        println(" {");

        skel.emitNext();

        if (scanner.scanErrorException == null) {
            println("    throw new Error(message);");
        } else {
            println("    throw new " + scanner.scanErrorException + "(message);");
        }

        skel.emitNext();

        print(" const yypushback = function(number) ");
        println(" {");
    }

    private void emitMain() {
        return;
    }

    private void emitNoMatch() {
        println("            zzScanError(ZZ_NO_MATCH);");
    }

    private void emitNextInput() {
        println("          if (zzCurrentPosL < zzEndReadL)");
        println("            zzInput = zzBufferL.charCodeAt(zzCurrentPosL++);");
        println("          else if (me.zzAtEOF) {");
        println("            zzInput = me.YYEOF;");
        println("            break zzForAction;");
        println("          }");
        println("          else {");
        println("            // store back cached positions");
        println("            me.zzCurrentPos  = zzCurrentPosL;");
        println("            me.zzMarkedPos   = zzMarkedPosL;");
        println("            var eof = zzRefill();");
        println("            // get translated positions and possibly new buffer");
        println("            zzCurrentPosL  = me.zzCurrentPos;");
        println("            zzMarkedPosL   = me.zzMarkedPos;");
        println("            zzBufferL      = me.zzBuffer;");
        println("            zzEndReadL     = me.zzEndRead;");
        println("            if (eof) {");
        println("              zzInput = me.YYEOF;");
        println("              break zzForAction;");
        println("            }");
        println("            else {");
        println("              zzInput = zzBufferL.charCodeAt(zzCurrentPosL++);");
        println("            }");
        println("          }");
    }

    private void emitHeader() {
        println("/* The following code was generated by JFlex " + Main.version + " on " + date + " */");
        println("");
    }

    private void emitUserCode() {
        if (scanner.userCode.length() > 0) {
            println(scanner.userCode.toString());
        }
    }

    private void emitClassName() {
        if (!endsWithJavadoc(scanner.userCode)) {
            String path = inputFile.toString();
            // slashify path (avoid backslash u sequence = unicode escape)
            if (File.separatorChar != '/') {
                path = path.replace(File.separatorChar, '/');
            }

            println("/**");
            println(" * This class is a scanner generated by ");
            println(" * <a href=\"http://www.jflex.de/\">JFlex</a> " + Main.version);
            println(" * on " + date + " from the specification file");
            println(" * <tt>" + path + "</tt>");
            println(" */");
        }


        print("function ");
        print(scanner.className);
        print("(inputString)");


        println(" {");
    }

    /**
     * Try to find out if user code ends with a javadoc comment 
     * 
     * @param usercode  the user code
     * @return true     if it ends with a javadoc comment
     */
    public static boolean endsWithJavadoc(StringBuilder usercode) {
        String s = usercode.toString().trim();

        if (!s.endsWith("*/")) {
            return false;
        }

        // find beginning of javadoc comment   
        int i = s.lastIndexOf("/**");
        if (i < 0) {
            return false;
        }

        // javadoc comment shouldn't contain a comment end
        return s.substring(i, s.length() - 2).indexOf("*/") < 0;
    }

    private void emitLexicalStates() {
        for (String name : scanner.states.names()) {
            int num = scanner.states.getNumber(name);

            println("const " + name + " = " + 2 * num + ";");
        }

        // can't quite get rid of the indirection, even for non-bol lex states: 
        // their DFA states might be the same, but their EOF actions might be different
        // (see bug #1540228)
        println("");
        println("/**");
        println(" * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l");
        println(" * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l");
        println(" *                  at the beginning of a line");
        println(" * l is of the form l = 2*k, k a non negative integer");
        println(" */");
        println("const ZZ_LEXSTATE = [ ");

        int i, j = 0;
        print("    ");

        for (i = 0; i < 2 * dfa.numLexStates - 1; i++) {
            print(dfa.entryState[i], 2);

            print(", ");

            if (++j >= 16) {
                println();
                print("    ");
                j = 0;
            }
        }

        println(dfa.entryState[i]);
        println("  ];");
    }

    private void emitDynamicInit() {
        int count = 0;
        int value = dfa.table[0][0];

        println("/** ");
        println(" * The transition table of the DFA");
        println(" */");

        CountEmitter e = new CountEmitter("Trans");
        e.setValTranslation(+1); // allow vals in [-1, 0xFFFE]
        e.emitInit();

        for (int i = 0; i < dfa.numStates; i++) {
            if (!rowKilled[i]) {
                for (int c = 0; c < dfa.numInput; c++) {
                    if (!colKilled[c]) {
                        if (dfa.table[i][c] == value) {
                            count++;
                        } else {
                            e.emit(count, value);

                            count = 1;
                            value = dfa.table[i][c];
                        }
                    }
                }
            }
        }

        e.emit(count, value);
        e.emitUnpack();

        println(e.toString());
    }

    private void emitCharMapInitFunction() {

        CharClasses cl = parser.getCharClasses();

        if (cl.getMaxCharCode() < 256) {
            return;
        }

        println("");
        println("/** ");
        println(" * Unpacks the compressed character translation table.");
        println(" *");
        println(" * @param packed   the packed character translation table");
        println(" * @return         the unpacked character translation table");
        println(" */");
        println("const zzUnpackCMap = function(packed) {");
        println("    var map = [];");
        println("    var i = 0;  /* index in packed string  */");
        println("    var j = 0;  /* index in unpacked array */");
        println("    while (i < " + 2 * intervals.length + ") {");
        println("      var  count = packed.charCodeAt(i++);");
        println("      var value = packed.charCodeAt(i++);");
        println("      do map[j++] = value; while (--count > 0);");
        println("    }");
        println("    return map;");
        println("};");
    }

    private void emitZZTrans() {

        int i, c;
        int n = 0;

        println("/** ");
        println(" * The transition table of the DFA");
        println(" */");
        println("const ZZ_TRANS [] = [");

        print("    ");
        for (i = 0; i < dfa.numStates; i++) {

            if (!rowKilled[i]) {
                for (c = 0; c < dfa.numInput; c++) {
                    if (!colKilled[c]) {
                        if (n >= 10) {
                            println();
                            print("    ");
                            n = 0;
                        }
                        print(dfa.table[i][c]);
                        if (i != dfa.numStates - 1 || c != dfa.numInput - 1) {
                            print(", ");
                        }
                        n++;
                    }
                }
            }
        }

        println();
        println("];");
    }

    private void emitCharMapArrayUnPacked() {

        CharClasses cl = parser.getCharClasses();

        println("");
        println("/** ");
        println(" * Translates characters to character classes");
        println(" */");
        println("const ZZ_CMAP = [");

        int n = 0;  // numbers of entries in current line    
        print("    ");

        int max = cl.getMaxCharCode();

        // not very efficient, but good enough for <= 255 characters
        for (char c = 0; c <= max; c++) {
            print(colMap[cl.getClassCode(c)], 2);

            if (c < max) {
                print(", ");
                if (++n >= 16) {
                    println();
                    print("    ");
                    n = 0;
                }
            }
        }

        println();
        println("];");
        println();
    }

    private void emitCharMapArray() {
        CharClasses cl = parser.getCharClasses();

        if (cl.getMaxCharCode() < 256) {
            emitCharMapArrayUnPacked();
            return;
        }

        // ignores cl.getMaxCharCode(), emits all intervals instead

        intervals = cl.getIntervals();

        println("");
        println("/** ");
        println(" * Translates characters to character classes");
        println(" */");
        println("const ZZ_CMAP_PACKED = ");

        int n = 0;  // numbers of entries in current line    
        print("    \"");

        int i = 0;
        int count, value;
        while (i < intervals.length) {
            count = intervals[i].end - intervals[i].start + 1;
            value = colMap[intervals[i].charClass];

            // count could be >= 0x10000
            while (count > 0xFFFF) {
                printUC(0xFFFF);
                printUC(value);
                count -= 0xFFFF;
                n++;
            }

            printUC(count);
            printUC(value);

            if (i < intervals.length - 1) {
                if (++n >= 10) {
                    println("\"+");
                    print("    \"");
                    n = 0;
                }
            }

            i++;
        }

        println("\";");
        println();
        
        
        emitCharMapInitFunction();
        println();

        println("/** ");
        println(" * Translates characters to character classes");
        println(" */");
        println("const ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);");
        println();
    }

    /**
     * Print number as octal/unicode escaped string character.
     * 
     * @param c   the value to print
     * @prec  0 <= c <= 0xFFFF 
     */
    private void printUC(int c) {
        if (c > 255) {
            out.print("\\u");
            if (c < 0x1000) {
                out.print("0");
            }
            out.print(Integer.toHexString(c));
        } else {
            out.print("\\");
            out.print(Integer.toOctalString(c));
        }
    }

    private void emitRowMapArray() {
        println("");
        println("/** ");
        println(" * Translates a state to a row index in the transition table");
        println(" */");

        HiLowEmitter e = new HiLowEmitter("RowMap");
        e.emitInit();
        for (int i = 0; i < dfa.numStates; i++) {
            e.emit(rowMap[i] * numCols);
        }
        e.emitUnpack();
        println(e.toString());
    }

    private void emitAttributes() {
        println("/**");
        println(" * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>");
        println(" */");

        CountEmitter e = new CountEmitter("Attribute");
        e.emitInit();

        int count = 1;
        int value = 0;
        if (dfa.isFinal[0]) {
            value = FINAL;
        }
        if (!isTransition[0]) {
            value |= NOLOOK;
        }

        for (int i = 1; i < dfa.numStates; i++) {
            int attribute = 0;
            if (dfa.isFinal[i]) {
                attribute = FINAL;
            }
            if (!isTransition[i]) {
                attribute |= NOLOOK;
            }

            if (value == attribute) {
                count++;
            } else {
                e.emit(count, value);
                count = 1;
                value = attribute;
            }
        }

        e.emit(count, value);
        e.emitUnpack();

        println(e.toString());
    }

    private void emitClassCode() {
        if (scanner.classCode != null) {
            println("  /* user code: */");
            println(scanner.classCode);
        }
    }

    private void emitDoEOF() {
        if (scanner.eofCode == null) {
            return;
        }

        println("/**");
        println(" * Contains user EOF-code, which will be executed exactly once,");
        println(" * when the end of file is reached");
        println(" */");

        print("  const zzDoEOF = function() {");
        println("    if (!me.zzEOFDone) {");
        println("      me.zzEOFDone = true;");
        println("    " + scanner.eofCode);
        println("    }");
        println("};");
        println("");
        println("");
    }

    private void emitLexFunctHeader() {

        print(" const ");

        /*if (scanner.tokenType == null) {
        if (scanner.isInteger) {
        print("int");
        } else if (scanner.isIntWrap) {
        print("Integer");
        } else {
        print("Yytoken");
        }
        } else {
        print(scanner.tokenType);
        }
        
        print(" ");
         */


        print(scanner.functionName);
        
        print(" = ");
        print("this.");
        print(scanner.functionName);
        

        print(" = function()");

        /*if (scanner.lexThrow != null) {
        print(", ");
        print(scanner.lexThrow);
        }
        
        if (scanner.scanErrorException != null) {
        print(", ");
        print(scanner.scanErrorException);
        }*/

        println(" {");

        skel.emitNext();

        if (scanner.useRowMap) {
            println("    var zzTransL = ZZ_TRANS;");
            println("    var zzRowMapL = ZZ_ROWMAP;");
            println("    var zzAttrL = ZZ_ATTRIBUTE;");

        }

        skel.emitNext();

        if (scanner.charCount) {
            println("      me.yychar += zzMarkedPosL - me.zzStartRead;");
            println("");
        }

        if (scanner.lineCount || scanner.columnCount) {
            println("      var zzR = false;");
            println("      for (zzCurrentPosL = me.zzStartRead; zzCurrentPosL < zzMarkedPosL;");
            println("                                                             zzCurrentPosL++) {");
            println("        switch (zzBufferL[zzCurrentPosL]) {");
            println("        case '\\u000B':");
            println("        case '\\u000C':");
            println("        case '\\u0085':");
            println("        case '\\u2028':");
            println("        case '\\u2029':");
            if (scanner.lineCount) {
                println("          me.yyline++;");
            }
            if (scanner.columnCount) {
                println("          me.yycolumn = 0;");
            }
            println("          zzR = false;");
            println("          break;");
            println("        case '\\r':");
            if (scanner.lineCount) {
                println("          me.yyline++;");
            }
            if (scanner.columnCount) {
                println("          me.yycolumn = 0;");
            }
            println("          zzR = true;");
            println("          break;");
            println("        case '\\n':");
            println("          if (zzR)");
            println("            zzR = false;");
            println("          else {");
            if (scanner.lineCount) {
                println("            me.yyline++;");
            }
            if (scanner.columnCount) {
                println("            me.yycolumn = 0;");
            }
            println("          }");
            println("          break;");
            println("        default:");
            println("          zzR = false;");
            if (scanner.columnCount) {
                println("          me.yycolumn++;");
            }
            println("        }");
            println("      }");
            println();

            if (scanner.lineCount) {
                println("      if (zzR) {");
                println("        // peek one character ahead if it is \\n (if we have counted one line too much)");
                println("        var zzPeek;");
                println("        if (zzMarkedPosL < zzEndReadL)");
                println("          zzPeek = zzBufferL[zzMarkedPosL] == '\\n';");
                println("        else if (me.zzAtEOF)");
                println("          zzPeek = false;");
                println("        else {");
                println("          var eof = zzRefill();");
                println("          zzEndReadL = me.zzEndRead;");
                println("          zzMarkedPosL = me.zzMarkedPos;");
                println("          zzBufferL = me.zzBuffer;");
                println("          if (eof) ");
                println("            zzPeek = false;");
                println("          else ");
                println("            zzPeek = zzBufferL[zzMarkedPosL] == '\\n';");
                println("        }");
                println("        if (zzPeek) me.yyline--;");
                println("      }");
            }
        }

        if (scanner.bolUsed) {
            // zzMarkedPos > zzStartRead <=> last match was not empty
            // if match was empty, last value of zzAtBOL can be used
            // zzStartRead is always >= 0
            println("      if (zzMarkedPosL > me.zzStartRead) {");
            println("        switch (zzBufferL[zzMarkedPosL - 1]) {");
            println("        case '\\n':");
            println("        case '\\u000B':");
            println("        case '\\u000C':");
            println("        case '\\u0085':");
            println("        case '\\u2028':");
            println("        case '\\u2029':");
            println("          me.zzAtBOL = true;");
            println("          break;");
            println("        case '\\r': ");
            println("          if (zzMarkedPosL < zzEndReadL)");
            println("            me.zzAtBOL = zzBufferL[zzMarkedPosL] != '\\n';");
            println("          else if (me.zzAtEOF)");
            println("            me.zzAtBOL = false;");
            println("          else {");
            println("            var eof = zzRefill();");
            println("            zzMarkedPosL = me.zzMarkedPos;");
            println("            zzEndReadL = me.zzEndRead;");
            println("            zzBufferL = me.zzBuffer;");
            println("            if (eof) ");
            println("              me.zzAtBOL = false;");
            println("            else ");
            println("              me.zzAtBOL = zzBufferL[zzMarkedPosL] != '\\n';");
            println("          }");
            println("          break;");
            println("        default:");
            println("          me.zzAtBOL = false;");
            println("        }");
            println("      }");
        }

        skel.emitNext();

        if (scanner.bolUsed) {
            println("      if (me.zzAtBOL)");
            println("        me.zzState = ZZ_LEXSTATE[me.zzLexicalState + 1];");
            println("      else");
            println("        me.zzState = ZZ_LEXSTATE[me.zzLexicalState];");
            println();
        } else {
            println("      me.zzState = ZZ_LEXSTATE[me.zzLexicalState];");
            println();
        }

        if (scanner.useRowMap) {
            println("      // set up zzAction for empty match case:");
            println("      var zzAttributes = zzAttrL[me.zzState];");
            println("      if ( (zzAttributes & 1) == 1 ) {");
            println("        zzAction = me.zzState;");
            println("      }");
            println();
        }

        skel.emitNext();
    }

    private void emitGetRowMapNext() {
        println("          var zzNext = zzTransL[ zzRowMapL[me.zzState] + zzCMapL[zzInput] ];");
        println("          if (zzNext == " + DFA.NO_TARGET + ") break zzForAction;");
        println("          me.zzState = zzNext;");
        println();

        println("          zzAttributes = zzAttrL[me.zzState];");

        println("          if ( (zzAttributes & " + FINAL + ") == " + FINAL + " ) {");

        skel.emitNext();

        println("            if ( (zzAttributes & " + NOLOOK + ") == " + NOLOOK + " ) break zzForAction;");

        skel.emitNext();
    }

    private void emitTransitionTable() {
        transformTransitionTable();

        println("          zzInput = zzCMapL[zzInput];");
        println();

        println("          var zzIsFinal = false;");
        println("          var zzNoLookAhead = false;");
        println();

        println("          zzForNext: { switch (me.zzState) {");

        for (int state = 0; state < dfa.numStates; state++) {
            if (isTransition[state]) {
                emitState(state);
            }
        }

        println("            default:");
        println("              // if this is ever reached, there is a serious bug in JFlex");
        println("              zzScanError(ZZ_UNKNOWN_ERROR);");
        println("              break;");
        println("          } }");
        println();

        println("          if ( zzIsFinal ) {");

        skel.emitNext();

        println("            if ( zzNoLookAhead ) break zzForAction;");

        skel.emitNext();
    }

    /**
     * Escapes all " ' \ tabs and newlines
     * 
     * @param s The string to escape
     * @return The escaped string
     */
    private String escapify(String s) {
        StringBuilder result = new StringBuilder(s.length() * 2);

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\'':
                    result.append("\\\'");
                    break;
                case '\"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\r':
                    if (i + 1 == s.length() || s.charAt(i + 1) != '\n') {
                        result.append("\"+ZZ_NL+\"");
                    }
                    break;
                case '\n':
                    result.append("\"+ZZ_NL+\"");
                    break;
                default:
                    result.append(c);
            }
        }

        return result.toString();
    }

    public void emitActionTable() {
        int lastAction = 1;
        int count = 0;
        int value = 0;

        println("  /** ");
        println("   * Translates DFA states to action switch labels.");
        println("   */");
        CountEmitter e = new CountEmitter("Action");
        e.emitInit();

        for (int i = 0; i < dfa.numStates; i++) {
            int newVal = 0;
            if (dfa.isFinal[i]) {
                Action action = dfa.action[i];
                if (action.isEmittable()) {
                    Integer stored = actionTable.get(action);
                    if (stored == null) {
                        stored = lastAction++;
                        actionTable.put(action, stored);
                    }
                    newVal = stored;
                }
            }

            if (value == newVal) {
                count++;
            } else {
                if (count > 0) {
                    e.emit(count, value);
                }
                count = 1;
                value = newVal;
            }
        }

        if (count > 0) {
            e.emit(count, value);
        }

        e.emitUnpack();
        println(e.toString());
    }

    private void emitActions() {
        println("      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {");

        int i = actionTable.size() + 1;

        for (Map.Entry<Action, Integer> entry : actionTable.entrySet()) {
            Action action = entry.getKey();
            int label = entry.getValue();

            println("        case " + label + ": ");

            if (action.lookAhead() == Action.FIXED_BASE) {
                println("          // lookahead expression with fixed base length");
                println("          me.zzMarkedPos = me.zzStartRead + " + action.getLookLength() + ";");
            }

            if (action.lookAhead() == Action.FIXED_LOOK
                || action.lookAhead() == Action.FINITE_CHOICE) {
                println("          // lookahead expression with fixed lookahead length");
                println("          yypushback(" + action.getLookLength() + ");");
            }

            if (action.lookAhead() == Action.GENERAL_LOOK) {
                println("          // general lookahead, find correct zzMarkedPos");
                println("          { var zzFState = " + dfa.entryState[action.getEntryState()] + ";");
                println("            var zzFPos = me.zzStartRead;");
                println("            if (zzFin.length <= zzBufferL.length) { zzFin = []; }");
                println("            var zzFinL[] = zzFin;");
                println("            while (zzFState != -1 && zzFPos < me.zzMarkedPos) {");
                println("              if ((zzAttrL[zzFState] & 1) == 1) { zzFinL[zzFPos] = true; } ");
                println("              zzInput = zzBufferL.charCodeAt(zzFPos++);");
                println("              zzFState = zzTransL[ zzRowMapL[zzFState] + zzCMapL[zzInput] ];");
                println("            }");
                println("            if (zzFState != -1 && (zzAttrL[zzFState] & 1) == 1) { zzFinL[zzFPos] = true; } ");
                println();
                println("            zzFState = " + dfa.entryState[action.getEntryState() + 1] + ";");
                println("            zzFPos = me.zzMarkedPos;");
                println("            while (!zzFinL[zzFPos] || (zzAttrL[zzFState] & 1) != 1) {");
                println("              zzInput = zzBufferL.charCodeAt(--zzFPos);");
                println("              zzFState = zzTransL[ zzRowMapL[zzFState] + zzCMapL[zzInput] ];");
                println("            };");
                println("            me.zzMarkedPos = zzFPos;");
                println("          }");
            }

            if (scanner.debugOption) {
                print("          System.out.println(");
                if (scanner.lineCount) {
                    print("\"line: \" + (me.yyline + 1) +\" \"+");
                }
                if (scanner.columnCount) {
                    print("\"col: \" + (me.yycolumn + 1) +\" \"+");
                }
                println("\"match: --\" + yytext() +\"--\");");
                print("          System.out.println(\"action [" + action.priority + "] { ");
                print(escapify(action.content));
                println(" }\");");
            }

            println("          { " + action.content);
            println("          }");
            println("        case " + (i++) + ": break;");
        }
    }

    private void emitEOFVal() {
        EOFActions eofActions = parser.getEOFActions();

        if (scanner.eofCode != null) {
            println("            zzDoEOF();");
        }

        if (eofActions.numActions() > 0) {
            println("            switch (me.zzLexicalState) {");

            // pick a start value for break case labels. 
            // must be larger than any value of a lex state:
            int last = dfa.numStates;

            for (String name : scanner.states.names()) {
                int num = scanner.states.getNumber(name);
                Action action = eofActions.getAction(num);

                if (action != null) {
                    println("            case " + name + ": {");
                    if (scanner.debugOption) {
                        print("              System.out.println(");
                        if (scanner.lineCount) {
                            print("\"line: \" + (me.yyline + 1) + \" \"+");
                        }
                        if (scanner.columnCount) {
                            print("\"col: \" + (me.yycolumn + 1) + \" \"+");
                        }
                        println("\"match: <<EOF>>\");");
                        print("              System.out.println(\"action [" + action.priority + "] { ");
                        print(escapify(action.content));
                        println(" }\");");
                    }
                    println("              " + action.content);
                    println("            }");
                    println("            case " + (++last) + ": break;");
                }
            }

            println("            default:");
        }

        Action defaultAction = eofActions.getDefault();

        if (defaultAction != null) {
            println("              {");
            if (scanner.debugOption) {
                print("                System.out.println(");
                if (scanner.lineCount) {
                    print("\"line: \" + (me.yyline + 1) +\" \"+");
                }
                if (scanner.columnCount) {
                    print("\"col: \" + (me.yycolumn + 1) +\" \"+");
                }
                println("\"match: <<EOF>>\");");
                print("                System.out.println(\"action [" + defaultAction.priority + "] { ");
                print(escapify(defaultAction.content));
                println(" }\");");
            }
            println("                " + defaultAction.content);
            println("              }");
        } else if (scanner.eofVal != null) {
            println("              { " + scanner.eofVal + " }");
        } else if (scanner.isInteger) {
            if (scanner.tokenType != null) {
                Out.error(ErrorMessages.INT_AND_TYPE);
                throw new GeneratorException();
            }
            println("            return me.YYEOF;");
        } else {
            println("            return null;");
        }

        if (eofActions.numActions() > 0) {
            println("            }");
        }
    }

    private void emitState(int state) {

        println("            case " + state + ":");
        println("              switch (zzInput) {");

        int defaultTransition = getDefaultTransition(state);

        for (int next = 0; next < dfa.numStates; next++) {

            if (next != defaultTransition && table[state][next] != null) {
                emitTransition(state, next);
            }
        }

        if (defaultTransition != DFA.NO_TARGET && noTarget[state] != null) {
            emitTransition(state, DFA.NO_TARGET);
        }

        emitDefaultTransition(state, defaultTransition);

        println("              }");
        println("");
    }

    private void emitTransition(int state, int nextState) {

        CharSetEnumerator chars;

        if (nextState != DFA.NO_TARGET) {
            chars = table[state][nextState].characters();
        } else {
            chars = noTarget[state].characters();
        }

        print("                case ");
        print(chars.nextElement());
        print(": ");

        while (chars.hasMoreElements()) {
            println();
            print("                case ");
            print(chars.nextElement());
            print(": ");
        }

        if (nextState != DFA.NO_TARGET) {
            if (dfa.isFinal[nextState]) {
                print("zzIsFinal = true; ");
            }

            if (!isTransition[nextState]) {
                print("zzNoLookAhead = true; ");
            }

            if (nextState == state) {
                println("break zzForNext;");
            } else {
                println("me.zzState = " + nextState + "; break zzForNext;");
            }
        } else {
            println("break zzForAction;");
        }
    }

    private void emitDefaultTransition(int state, int nextState) {
        print("                default: ");

        if (nextState != DFA.NO_TARGET) {
            if (dfa.isFinal[nextState]) {
                print("zzIsFinal = true; ");
            }

            if (!isTransition[nextState]) {
                print("zzNoLookAhead = true; ");
            }

            if (nextState == state) {
                println("break zzForNext;");
            } else {
                println("me.zzState = " + nextState + "; break zzForNext;");
            }
        } else {
            println("break zzForAction;");
        }
    }

    private int getDefaultTransition(int state) {
        int max = 0;

        for (int i = 0; i < dfa.numStates; i++) {
            if (table[state][max] == null) {
                max = i;
            } else if (table[state][i] != null && table[state][max].size() < table[state][i].size()) {
                max = i;
            }
        }

        if (table[state][max] == null) {
            return DFA.NO_TARGET;
        }
        if (noTarget[state] == null) {
            return max;
        }

        if (table[state][max].size() < noTarget[state].size()) {
            max = DFA.NO_TARGET;
        }

        return max;
    }

    // for switch statement:
    private void transformTransitionTable() {

        int numInput = parser.getCharClasses().getNumClasses() + 1;

        int i;
        char j;

        table = new CharSet[dfa.numStates][dfa.numStates];
        noTarget = new CharSet[dfa.numStates];

        for (i = 0; i < dfa.numStates; i++) {
            for (j = 0; j < dfa.numInput; j++) {

                int nextState = dfa.table[i][j];

                if (nextState == DFA.NO_TARGET) {
                    if (noTarget[i] == null) {
                        noTarget[i] = new CharSet(numInput, colMap[j]);
                    } else {
                        noTarget[i].add(colMap[j]);
                    }
                } else {
                    if (table[i][nextState] == null) {
                        table[i][nextState] = new CharSet(numInput, colMap[j]);
                    } else {
                        table[i][nextState].add(colMap[j]);
                    }
                }
            }
        }
    }

    private void findActionStates() {
        isTransition = new boolean[dfa.numStates];

        for (int i = 0; i < dfa.numStates; i++) {
            char j = 0;
            while (!isTransition[i] && j < dfa.numInput) {
                isTransition[i] = dfa.table[i][j++] != DFA.NO_TARGET;
            }
        }
    }

    private void reduceColumns() {
        colMap = new int[dfa.numInput];
        colKilled = new boolean[dfa.numInput];

        int i, j, k;
        int translate = 0;
        boolean equal;

        numCols = dfa.numInput;

        for (i = 0; i < dfa.numInput; i++) {

            colMap[i] = i - translate;

            for (j = 0; j < i; j++) {

                // test for equality:
                k = -1;
                equal = true;
                while (equal && ++k < dfa.numStates) {
                    equal = dfa.table[k][i] == dfa.table[k][j];
                }

                if (equal) {
                    translate++;
                    colMap[i] = colMap[j];
                    colKilled[i] = true;
                    numCols--;
                    break;
                } // if
            } // for j
        } // for i
    }

    private void reduceRows() {
        rowMap = new int[dfa.numStates];
        rowKilled = new boolean[dfa.numStates];

        int i, j, k;
        int translate = 0;
        boolean equal;

        numRows = dfa.numStates;

        // i is the state to add to the new table
        for (i = 0; i < dfa.numStates; i++) {

            rowMap[i] = i - translate;

            // check if state i can be removed (i.e. already
            // exists in entries 0..i-1)
            for (j = 0; j < i; j++) {

                // test for equality:
                k = -1;
                equal = true;
                while (equal && ++k < dfa.numInput) {
                    equal = dfa.table[i][k] == dfa.table[j][k];
                }

                if (equal) {
                    translate++;
                    rowMap[i] = rowMap[j];
                    rowKilled[i] = true;
                    numRows--;
                    break;
                } // if
            } // for j
        } // for i

    }

    /**
     * Set up EOF code section according to scanner.eofcode 
     */
    private void setupEOFCode() {
        if (scanner.eofclose) {
            scanner.eofCode = LexScan.conc(scanner.eofCode, "  yyclose();");
            scanner.eofThrow = "";//LexScan.concExc(scanner.eofThrow, "java.io.IOException");
        }
    }

    /**
     * Main JavaEmitter method.  
     */
    public void emit() {

        setupEOFCode();

        if (scanner.functionName == null) {
            scanner.functionName = "yylex";
        }

        reduceColumns();
        findActionStates();

        emitHeader();
        //println("const ZZ_BUFFERSIZE = " + scanner.bufferSize + ";");
        if (scanner.debugOption) {
            println("const ZZ_NL = '\n';");
        }
        skel.emitNext();
        emitLexicalStates();
        emitCharMapArray();
        emitActionTable();

        if (scanner.useRowMap) {
            reduceRows();
            emitRowMapArray();
            if (scanner.packed) {
                emitDynamicInit();
            } else {
                emitZZTrans();
            }
        }
        

        if (scanner.useRowMap) {
            emitAttributes();
        }

        println("");

        emitUserCode();
        emitClassName();

        skel.emitNext();
        skel.emitNext();
        skel.emitNext();

        emitLookBuffer();

        emitClassCode();

        skel.emitNext();
        skel.emitNext();

        emitScanError();

        skel.emitNext();

        emitDoEOF();

        skel.emitNext();

        emitLexFunctHeader();

        emitNextInput();

        if (scanner.useRowMap) {
            emitGetRowMapNext();
        } else {
            emitTransitionTable();
        }

        skel.emitNext();

        emitActions();

        skel.emitNext();

        emitEOFVal();

        skel.emitNext();

        emitNoMatch();

        skel.emitNext();

        emitMain();

        skel.emitNext();

        out.close();
    }

    protected abstract static class PackEmitter {

        /** name of the generated array (mixed case, no yy prefix) */
        protected String name;

        /** current UTF8 length of generated string in current chunk */
        private int UTF8Length;

        /** position in the current line */
        private int linepos;

        /** max number of entries per line */
        private static final int maxEntries = 16;

        /** output buffer */
        protected StringBuilder out = new StringBuilder();

        /** number of existing string chunks */
        protected int chunks;

        /** maximum size of chunks */
        // String constants are stored as UTF8 with 2 bytes length
        // field in class files. One Unicode char can be up to 3 
        // UTF8 bytes. 64K max and two chars safety. 
        private static final int maxSize = 0xFFFF - 6;

        /** indent for string lines */
        private static final String indent = "    ";

        /**
         * Create new emitter for an array.
         * 
         * @param name  the name of the generated array
         */
        protected PackEmitter(String name) {
            this.name = name;
        }

        /**
         * Convert array name into all uppercase internal scanner 
         * constant name.
         * 
         * @return <code>name</code> as a internal constant name.
         * @see PackEmitter#name
         */
        protected String constName() {
            return "ZZ_" + name.toUpperCase();
        }

        /**
         * Return current output buffer.
         */
        public String toString() {
            return out.toString();
        }

        /**
         * Emit declaration of decoded member and open first chunk.
         */
        public void emitInit() {
            nl();
            nextChunk();
        }

        /**
         * Emit single unicode character. 
         * 
         * Updates length, position, etc.
         *
         * @param i  the character to emit.
         * @prec  0 <= i <= 0xFFFF 
         */
        public void emitUC(int i) {
            if (i < 0 || i > 0xFFFF) {
                throw new IllegalArgumentException("character value expected");
            }

            // cast ok because of prec  
            char c = (char) i;

            printUC(c);
            UTF8Length += UTF8Length(c);
            linepos++;
        }

        /**
         * Execute line/chunk break if necessary. 
         * Leave space for at least two chars.
         */
        public void breaks() {
            if (UTF8Length >= maxSize) {
                // close current chunk
                out.append("\";");
                nl();

                nextChunk();
            } else {
                if (linepos >= maxEntries) {
                    // line break
                    out.append("\"+");
                    nl();
                    out.append(indent);
                    out.append("\"");
                    linepos = 0;
                }
            }
        }

        /**
         * Emit the unpacking code. 
         */
        public abstract void emitUnpack();

        /**
         *  emit next chunk 
         */
        private void nextChunk() {
            nl();
            out.append("const ");
            out.append(constName());
            out.append("_PACKED_");
            out.append(chunks);
            out.append(" =");
            nl();
            out.append(indent);
            out.append("\"");

            UTF8Length = 0;
            linepos = 0;
            chunks++;
        }

        /**
         *  emit newline 
         */
        protected void nl() {
            out.append(Out.NL);
        }

        /**
         * Append a unicode/octal escaped character 
         * to <code>out</code> buffer.
         * 
         * @param c the character to append
         */
        private void printUC(char c) {
            if (c > 255) {
                out.append("\\u");
                if (c < 0x1000) {
                    out.append("0");
                }
                out.append(Integer.toHexString(c));
            } else {
                out.append("\\");
                out.append(Integer.toOctalString(c));
            }
        }

        /**
         * Calculates the number of bytes a Unicode character
         * would have in UTF8 representation in a class file.
         *
         * @param value  the char code of the Unicode character
         * @prec  0 <= value <= 0xFFFF
         *
         * @return length of UTF8 representation.
         */
        private int UTF8Length(char value) {
            // if (value < 0 || value > 0xFFFF) throw new Error("not a char value ("+value+")");

            // see JVM spec section 4.4.7, p 111
            if (value == 0) {
                return 2;
            }
            if (value <= 0x7F) {
                return 1;
            }

            // workaround for javac bug (up to jdk 1.3):
            if (value < 0x0400) {
                return 2;
            }
            if (value <= 0x07FF) {
                return 3;
            }

            // correct would be:
            // if (value <= 0x7FF) return 2;
            return 3;
        }

        // convenience
        protected void println(String s) {
            out.append(s);
            nl();
        }
    };

    private static class CountEmitter extends PackEmitter {

        /** number of entries in expanded array */
        private int numEntries;

        /** translate all values by this amount */
        private int translate = 0;

        /**
         * Create a count/value emitter for a specific field.
         * 
         * @param name   name of the generated array
         */
        protected CountEmitter(String name) {
            super(name);
        }

        /**
         * Emits count/value unpacking code for the generated array. 
         * 
         * @see jflex.PackEmitter#emitUnpack()
         */
        public void emitUnpack() {
            // close last string chunk:
            println("\";");

            nl();
            println("const zzUnpack" + name + " = function() {");
            println("   var result = [];");
            println("   var offset = 0;");

            for (int i = 0; i < chunks; i++) {
                println("   offset = zzUnpack" + name + "Internal(" + constName() + "_PACKED_" + i + ", offset, result);");
            }

            println("   return result;");
            println("};");
            nl();

            println("function zzUnpack" + name + "Internal(packed, offset, result) {");
            println("   var i = 0;       /* index in packed string  */");
            println("   var j = offset;  /* index in unpacked array */");
            println("   var l = packed.length;");
            println("   while (i < l) {");
            println("      var count = packed.charCodeAt(i++);");
            println("      var value = packed.charCodeAt(i++);");
            if (translate == 1) {
                println("      value--;");
            } else if (translate != 0) {
                println("      value-= " + translate);
            }
            println("      do result[j++] = value; while (--count > 0);");
            println("   }");
            println("   return j;");
            println("};");

            nl();
            out.append("const ");
            out.append(constName());
            out.append(" = zzUnpack");
            out.append(name);
            out.append("();");
        }

        /**
         * Translate all values by given amount.
         * 
         * Use to move value interval from [0, 0xFFFF] to something different.
         * 
         * @param i   amount the value will be translated by. 
         *            Example: <code>i = 1</code> allows values in [-1, 0xFFFE].
         */
        public void setValTranslation(int i) {
            this.translate = i;
        }

        /**
         * Emit one count/value pair. 
         * 
         * Automatically translates value by the <code>translate</code> value. 
         * 
         * @param count
         * @param value
         * 
         * @see CountEmitter#setValTranslation(int)
         */
        public void emit(int count, int value) {
            numEntries += count;
            breaks();
            emitUC(count);
            emitUC(value + translate);
        }
    };

    private static class HiLowEmitter extends PackEmitter {

        /** number of entries in expanded array */
        private int numEntries;

        /**
         * Create new emitter for values in [0, 0xFFFFFFFF] using hi/low encoding.
         * 
         * @param name   the name of the generated array
         */
        private HiLowEmitter(String name) {
            super(name);
        }

        /**
         * Emits hi/low pair unpacking code for the generated array. 
         * 
         * @see jflex.PackEmitter#emitUnpack()
         */
        public void emitUnpack() {
            // close last string chunk:
            println("\";");
            nl();
            println("const zzUnpack" + name + " = function() {");
            println("   var result = [];");
            println("   var offset = 0;");

            for (int i = 0; i < chunks; i++) {
                println("   offset = zzUnpack" + name + "Internal(" + constName() + "_PACKED_" + i + ", offset, result);");
            }

            println("    return result;");
            println("};");

            nl();
            println("function zzUnpack" + name + "Internal(packed, offset, result) {");
            println("    var i = 0;  /* index in packed string  */");
            println("    var j = offset;  /* index in unpacked array */");
            println("    var l = packed.length;");
            println("    while (i < l) {");
            println("      var high = packed.charCodeAt(i++) << 16;");
            println("      result[j++] = high | packed.charCodeAt(i++);");
            println("    }");
            println("    return j;");
            println("};");


            nl();
            out.append("const ");
            out.append(constName());
            out.append(" = zzUnpack");
            out.append(name);
            out.append("();");
        }

        /**
         * Emit one value using two characters. 
         *
         * @param val  the value to emit
         * @prec  0 <= val <= 0xFFFFFFFF 
         */
        public void emit(int val) {
            numEntries += 1;
            breaks();
            emitUC(val >> 16);
            emitUC(val & 0xFFFF);
        }
    }
}
