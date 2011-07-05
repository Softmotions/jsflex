/*
 * The pre-processor performs processes metadata and prepares the
 * document for the full parsing by the processor.
 */

%%


%class JAMWikiPreProcessor
%type String
%unicode
%ignorecase

/* code copied verbatim into the generated .java file */
%{
    
%}

/* character expressions */
newline            = "\n"
whitespace         = {newline} | [ \t\f]

/* nowiki */
nowiki             = (<[ ]*nowiki[ ]*>) ~(<[ ]*\/[ ]*nowiki[ ]*>)

/* pre */
htmlpreattributes  = class|dir|id|lang|style|title
htmlpreattribute   = ([ ]+) {htmlpreattributes} ([ ]*=[^>\n]+[ ]*)*
htmlprestart       = (<[ ]*pre ({htmlpreattribute})* [ ]* (\/)? [ ]*>)
htmlpreend         = (<[ ]*\/[ ]*pre[ ]*>)
htmlpre            = ({htmlprestart}) ~({htmlpreend})
wikiprestart       = (" ")+ ([^ \t\n])
wikipreend         = ([^ ]) | ({newline})

/* processing commands */
noeditsection      = ({newline})? "__NOEDITSECTION__"

/* wiki links */
protocol           = "http://" | "https://" | "mailto:" | "mailto://" | "ftp://" | "file://"
htmllinkwiki       = "[" ({protocol}) ([^\]\n]+) "]"
htmllinkraw        = ({protocol}) ([^ <'\"\n\t]+)
htmllink           = ({htmllinkwiki}) | ({htmllinkraw})
wikilinkcontent    = [^\n\]] | "]" [^\n\]] | {htmllink}
wikilink           = "[[" ({wikilinkcontent})+ "]]" [a-z]*
nestedwikilink     = "[[" ({wikilinkcontent})+ "|" ({wikilinkcontent} | {wikilink})+ "]]"

%state WIKIPRE

%%

/* ----- nowiki ----- */

<YYINITIAL, WIKIPRE>{nowiki} {
    console.log("nowiki: " + yytext() + " (" + yystate() + ")");
    return yytext();
}

/* ----- pre ----- */

<YYINITIAL>{htmlpre} {
    console.log("htmlpre: " + yytext() + " (" + yystate() + ")");
    return yytext();
}

<YYINITIAL, WIKIPRE>^{wikiprestart} {
    console.log("wikiprestart: " + yytext() + " (" + yystate() + ")");
    // rollback the one non-pre character so it can be processed
    yypushback(yytext().length - 1);
    if (yystate() != WIKIPRE) {
        //beginState(WIKIPRE);
        yybegin(WIKIPRE);
    }
    return yytext();
}

<WIKIPRE>^{wikipreend} {
    console.log("wikipreend: " + yytext() + " (" + yystate() + ")");
    yybegin(YYINITIAL);
    // rollback the one non-pre character so it can be processed
    yypushback(1);
    return yytext();
}

/* ----- processing commands ----- */

<YYINITIAL>{noeditsection} {
    console.log("noeditsection: " + yytext() + " (" + yystate() + ")");
    //this.parserInput.setAllowSectionEdit(false);
    return yytext();//(this.mode < JFlexParser.MODE_PREPROCESS) ? yytext() : "";
}

/* ----- wiki links ----- */

<YYINITIAL>{wikilink} {
    console.log("wikilink: " + yytext() + " (" + yystate() + ")");
    return yytext();//this.parse(TAG_TYPE_WIKI_LINK, yytext());
}

<YYINITIAL>{nestedwikilink} {
    console.log("nestedwikilink: " + yytext() + " (" + yystate() + ")");
    return yytext();//this.parse(TAG_TYPE_WIKI_LINK, yytext(), "nested");
}

/* ----- other ----- */

<YYINITIAL, WIKIPRE>{whitespace} {
    // no need to log this
    return yytext();
}

<YYINITIAL, WIKIPRE>. {
    // no need to log this
    return yytext();
}
