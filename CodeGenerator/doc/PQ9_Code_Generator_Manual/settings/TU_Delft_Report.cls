% Not official, created by Erik Mekkes

% Load all default functionality from report
\LoadClass[11pt,twoside,a4paper]{report}
% utf8 input
\usepackage[utf8]{inputenc}
%\usepackage[a4paper,margin=2.54cm]{geometry}
\usepackage{a4wide,times}

% section title and pagenumber at top of pages
\pagestyle{headings}
% remove headers and footers on empty pages
\usepackage{emptypage}
% for changing amount of whitespace around titles
\usepackage{titlesec}
% chapter title format
\titleformat{\chapter}[display]
    {\normalfont\LARGE\bfseries}{\chaptertitlename\ \thechapter}{5pt}{\Huge}
\titlespacing*{\chapter}{0pt}{-20pt}{10pt}

% Images
\usepackage{graphicx} 
\usepackage{float}  % positioning
\usepackage{caption}
\usepackage{subcaption}
\graphicspath{ {images/} }

% Math packages
\usepackage{amsmath}
\usepackage{amsthm}
\usepackage{amssymb}

%Pseudocode
\usepackage{algorithm}
\usepackage[noend]{algpseudocode}
%You can also use the pseudocode package. http://cacr.uwaterloo.ca/~dstinson/papers/pseudocode.pdf
%\usepackage{pseudocode}

% English hyphenation
\usepackage[english]{babel}
% Used for APA references
\usepackage{csquotes}
\usepackage[style=apa,backend=biber]{biblatex}
\DeclareLanguageMapping{english}{english-apa}
\addbibresource{bibliography.bib}
% Add bibliography to Table of Contents
\usepackage{etoolbox}
\apptocmd{\printbibliography}{\addcontentsline{toc}{chapter}{\bibname}}{}{}

%use package for appendices
\usepackage{appendix}
\newcommand*{\Appendixautorefname}{Appendix}

% Hyperreferences in the pdf.
\usepackage{hyperref}

% Code Snippets using listing
\usepackage{xcolor} % for text colors
\usepackage{realboxes} % for background colors
\usepackage{listings}

% Load languages for efficiency
\lstloadlanguages{java}

% Define colours to use for code segments
\definecolor{string}{rgb}{0.7,0.0,0.0}
\definecolor{comment}{rgb}{0.13,0.54,0.13}
\definecolor{keyword}{rgb}{0.0,0.0,1.0}
\definecolor{backcolour}{rgb}{0.95,0.95,0.92}

% Global options for code snippet layout.
% used by placing code within lstlisting environment (begin{lstlisting}).
% or by using \lstinputlisting{hello.c}
\lstset{
	language=c,
	backgroundcolor=\color{backcolour},
	breaklines=true,
	basicstyle=\small,
	tabsize=2,
	stringstyle=\ttfamily\color{string},
	keywordstyle=\ttfamily\color{keyword},
	identifierstyle=\ttfamily,
	showstringspaces=false,
	commentstyle=\ttfamily\color{comment},
	numbers=none,
	numberstyle=\scriptsize
}

\usepackage{color,soul}
\sethlcolor{backcolour}