/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jflex;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;

/**
 *
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class JSTest extends TestCase {

    public void testJS() throws Exception {
    }

    public static void main(String[] args) throws Exception {        
        File inFile = new File("js-sample.flex");
        Main.generate(inFile, new EmitterFactory() {

            public IEmitter createEmitter(File inputFile, LexParse parser, DFA dfa) throws IOException {
                return new JSEmitter(inputFile, parser, dfa);
            }
        });
        
        Main.generate(inFile, null);
    }
}
