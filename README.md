The program is executed through the main entry point located at src/org/VOASC/MainSolve.class.

This project provides a unified framework for solving both CSP and COP. 

The core algorithm automatically switches between VOASC and VOASCcop based on the configuration flag(isCSP).

| isCSP Value     |   Algorithm Called  |      Description                 |

|      true       |      VOASC          |  Solve satisfaction problems     |

|      false      |     VOASCcop        |  Solve optimization problems     |
