/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jflex;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public interface EmitterFactory {

    IEmitter createEmitter(File inputFile, LexParse parser, DFA dfa) throws IOException;
}
