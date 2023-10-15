Sudoker helps you play and solve Sudokus with accelerated workflow.
It aims to combine the functionalities of a solver, generator, and sudoker player, along with many other side functions.
It is still in its primitive stages and will have new functions and features added.

Note: it's not being updated regularly, and needs a huge update for the Generator functions.
To do list:
 - Unique hash for each sodoku, but same for sudokers that are equivalent by symmetry
 - Sudoku database support
 - a Working generator that can generate/fetch desired sudoku in reasonable amount of time
 - more solving techniques and thus better accuracy when rating a sudoku's difficulty
 - AI support to import sodoku from images / camera
 - support for other platforms/operating systems
 - proper documentation, JavaDoc
 - it's own webpage

## Features

### Main features:
  - Instantly solves any sudoku
  - Find up to 100,000 solutions of a sudoku
  - Solve a sudoku step by step according to different solving techniques (so far there is only a few implemented)
  - 3 sudoku view/edit modes: show all candidates used in solver, show pencil marks made by the user, or blank for unsolved cells
  - Press the tab key to traverse to the next cell of a sudoku board
  - Sudokus can be exported to text or file and later imported back
  - Generate sudokus with clue number and location settings (later will have difficulty and other options too)
  - Convenient highlight options
  - Check a sudoku's validity (i.e. conflicts and number of solutions)

### Sudoker values user preferences, and supports preferences including the following:
  - Custom location for the folder to store application files
  - Custom application global font size
  - Different look and feels and metal themes support
  - Custom sudoku board visuals
  - Custom digits to display instead of 1-9
  - Custom keyboard shortcuts
  - Custom default order for solving techniques in step by step solving 
  - Setting for maximum number of solutions to look for for a sudoku puzzle, up to 100,000
  - Different colors and settings for History Trees
  - Option to automatically save opened sudokus when exiting, and reopen them the next time the application is opened

### Other Features:
  - History Tree shows non-linear history of changes made to a sudoku board
  - Board Comparator compares the differences between 2 boards
  - Has a music player that mostly plays .WAV files, and is not memory or CPU efficient.
  - Very basic print support for a sudoku board
  - Each sudoku board comes with a stopwatch
  - A Memory Monitor Panel that supposedly monitors application memory usage.
  - Provides a metal theme to resemble "dark mode"
 
## Download
The latest build is available for [download here.](https://github.com/Shayna003/sudoker/releases/latest)

You need to either have Java Runtime Environment(JRE) 10 or later installed, or choose a file that comes with an OpenJDK's JRE bundled.

## Screenshots

Step by Step Solving
![screenshot](https://user-images.githubusercontent.com/79242907/120547776-23989100-c424-11eb-818f-e9591b37f131.png)

History Trees
![screenshot](https://user-images.githubusercontent.com/79242907/120548418-e7b1fb80-c424-11eb-8e93-7eae37ebcb20.png)

Board Comparator
![screenshot](https://user-images.githubusercontent.com/79242907/120548426-e97bbf00-c424-11eb-8291-0e281ea26c88.png)

Generator
![screenshot](https://user-images.githubusercontent.com/79242907/120548819-6149e980-c425-11eb-98e6-108324a0788f.png)



