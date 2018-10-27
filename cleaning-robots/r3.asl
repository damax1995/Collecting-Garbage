// mars robot 3

/* Initial beliefs */

at(P) :- pos(P,X,Y) & pos(r3,X,Y).

/* Initial goal */

!move(slots).

/* Plans */

+!move(slot)
   <- next(slot);
   	move(slots).

+!move(slots).
