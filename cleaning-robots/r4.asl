// mars robot 4

/* Initial beliefs */

at(P) :- pos(P,X,Y) & pos(r4,X,Y).

/* Initial goal */

!move(slot).

/* Plans */

+!move(slot) : not garbage(r4)
   <-?pos(r3,X,Y); 
   	move_towards(X,Y, 3);
   	!move(slot).

+!move(slot).

+garbage(r4) : true <- burn(garb).
