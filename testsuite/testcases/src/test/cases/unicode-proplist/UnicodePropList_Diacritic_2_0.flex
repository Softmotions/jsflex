%%

%unicode 2.0
%public
%class UnicodePropList_Diacritic_2_0

%type int
%standalone

%include src/test/resources/common-unicode-binary-property-java

%%

\p{Diacritic} { setCurCharPropertyValue(); }
[^] { }

<<EOF>> { printOutput(); return 1; }
