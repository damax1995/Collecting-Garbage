// mars robot 3

/* Initial beliefs */

at(P) :- pos(P,X,Y) & pos(r3,X,Y).

/* Initial goal */

!move(slot).

/* Plans */

+!move(slot) : not garbage(r3)
   <- generate(garb);
   	move(slot);
   	!move(slot).

+!move(slot).
