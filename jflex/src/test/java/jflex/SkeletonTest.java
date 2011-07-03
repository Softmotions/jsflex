/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * JFlex 1.5                                                               *
 * Copyright (C) 1998-2008  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * License: BSD                                                            *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package jflex;


import java.io.File;

import junit.framework.TestCase;

/**
 * SkeletonTest
 * 
 * @author Gerwin Klein
 * @version $Revision$, $Date$
 */
public class SkeletonTest extends TestCase {

  /**
   * Constructor for SkeletonTest.
   * @param arg0 test name
   */
  public SkeletonTest(String arg0) {
    super(arg0);
  }

  public void testReplace() {
    assertEquals(Skeleton.replace("bla ", "blub", "bla blub bla "), 
                 "blubblub blub");
  }

  public void testMakePrivate() {
    Options.skel.makePrivate(); 
    for (int i=0; i < Options.skel.line.length; i++) {
      assertEquals(Options.skel.line[i].indexOf("public"), -1);
    }
  }

  public void testDefault() {
    Options.skel.readSkelFile(new File("src/main/jflex/skeleton.nested"));
    assertTrue(Options.skel.line[3].indexOf("java.util.Stack") > 0);
    Options.skel.readDefault();
    assertEquals(Options.skel.line[3].indexOf("java.util.Stack"), -1);
  }
}
