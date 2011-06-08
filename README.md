
Fenrir
======

`fenrir` is an alternative class-based object-system for Clojure based on hash-maps and multimethods.

Usage
-----

Simply add this to your leiningen deps: `[fenrir "0.1.0"]`

Documentation
-------------

The documentation can be found here: http://eduardoejp.github.com/fenrir/

Rationale
---------

To be honest, I don't have a good reason for creating Fenrir. Clojure already has an object system based on protocols,
records and types and I think that the whole idea of coding to interfaces instead of coding to concrete implementations is
great. I'm a big fan of Clojure protocols. I made Fenrir as some sort of entertaining side project that I could use to play around.

However, even though this was made mostly for fun, it's fully functional and has various interesting features.

Examples
--------

	(defclass fShape [] []
	  (get area -1)
	  (get perimeter -1))
	
	(defclass fSquare [user/fShape] [side]
	  (get area (* side side))
	  (get perimeter (* side 4)))
	
	(defclass fCircle [user/fShape] [radius]
	  (get diameter (* radius 2))
	  (get area (* Math/PI (* radius radius)))
	  (get perimeter (* Math/PI (get-slot *self* :diameter))))

This code illustrates some of the features of Fenrir: Slots, Virtual Slots, Inheritance (which can be multiple) & Polymorphism

Before I explain each feature, please evaluate the following sexps:

	(def s (ctor fShape))
	(get-slot s :area)
	(get-slot s :perimeter)

	(def sq (ctor fSquare :side 5))
	(get-slot sq :side)
	(get-slot sq :area)
	(get-slot sq :perimeter)

	(def cir (ctor fCircle :radius 6))
	(get-slot cir :radius)
	(get-slot cir :diameter)
	(get-slot cir :area)
	(get-slot cir :perimeter)

1. Slots in Fenrir can come in two forms: Normal Slots & Virtual Slots

*Normal Slots*: These are instance variables in your object. They can be called by name inside method definitions and inside getters, setters and virtual slot definitions.

*Virtual Slots*: These can be accessed like every other slot by outside users (although they cannot be called by name inside definitions). If someone tries to set them
  to a value through set-slot, they won't be able to and set-slot will return the original object. Accessing them through get-slot will result in the forms being
  evaluated and returning a value
2. Inheritance (can be multiple): All the fclasses that are being inherited must be namespace qualified. The subclass will then inherit all the normal and virtual slots
   from its base classes, along with all the methods. When two or more slots or virtual slots with the same name are being inherited by a class from its superclasses,
   an exception will be thrown by the defclass macro. The way to solve that problem is by declaring the inherited slot in the subclass and, if desired,
   "masking" the parent functionality in the child class (details on how to mask will be given later).
3. Polymorphism: There can be polymorphism with methods, getters (for either normal or virtual slots) and setters (only for normal slots).

Instantiation is done through the ctor method (which can be overwritten to create custom constructors). The get-slot method provides access (getters are created
  through the 'get' "special form"). The set-slot method provides mutation (setters are created through the 'set' "special form").
The default ctor implementation takes the object's slots as :keywords.
The `*self*` variable refers to the object (like 'this' in Java) inside method definitions, getters & setters.

Another example will help illustrate more features of Fenrir:

	(defclass fPet [] [name age owner]
	  "A pet of any kind."
	  (make-noise "Make some pet noise."))

	(defclass fDog [fenrir/fPet] []
	  "A dog of any race."
	  (set age (if (< *val* 15) *val* -1)) ; If the dog is too old, put it to sleep...
	  (get name (str owner "'s dog, " name))
	  (make-noise [] (println "Bark!")))

	(defclass fMachine [] [name manufacturer]
	  "A machine of some kind."
	  (get name (str name ", Copyright (C) " manufacturer " 2011"))
	  (explode [] (println "BOOM!")))

	(defclass fRoboDog [fenrir/fDog fenrir/fMachine] [name]
	  "fDog 2.0"
	  (ctor [name age owner manufacturer]
	    (base-ctor *fclass* :name name :age age :owner owner :manufacturer manufacturer))
	  (make-noise [] (println "*Buzz* Bark! *Buzz*"))
	  (mask name fenrir/fMachine))

Before explaining everything, try this:

	(def pet (ctor fPet :name "Claws" :age 5 :owner "Mr. Foo"))
	(make-noise pet)

	(def dog (ctor fDog :name "Rex" :age 5 :owner "Mr. Bar"))
	(make-noise dog)
	(get-slot dog :name)
	(set-slot dog :age 3)
	(set-slot dog :age 18)

	(def machine (ctor fMachine :name "IPhone" :manufacturer "Apple Inc."))
	(get-slot machine :manufacturer)
	(get-slot machine :name)

	(def robodog (ctor fRoboDog "TRex" 10 "Ms. Baz" "Bots R Us"))
	(make-noise robodog)
	(get-slot robodog :name)

Here we can see method definitions, getters & setters, multiple inheritance, slot clashing, ctor definition and masking.

1. Method Definitions: When provided only the method name or the name and a docstring, the method will be defined and the base implementation will throw an exception.
  When provided a definition, the method will be declared with that base definition.
  When implementing in a class an already existing method, a new implementation will be added for that class.
2. Mutators/Setters: They will be invoked upon calls to set-slot. They can have access to the value they are given via `*val*`.
  Their returning value will become the new value of the slot.
3. Multiple Inheritance: When 2 or more slots or virtual slots with the same name are being inherited from 2 or more different sources, the defclass macro will throw
  an exception. The slot will have to be declared in the inheriting class (as can be seen in fRoboDog). However, if you still wish to use the get/set implementation
  of the slot, you have to "mask it" with the 'mask' special form. It takes an slot, a virtual slot or a method and redirects calls to them to the given superclass'
  implementation. That way, you can derive functionality in whatever way you like from multiple superclasses' implementations without having to worry about the order
  of inheritance and stuff like that.
4. Constructor Definition: You define a constructor through the 'ctor' special form. It will always take an implicit `*fclass*` argument.
  Having a constructor allows you to transform the input or validate it when creating your object. After that, you can call base-class to invoke fObject's
  ctor implementation.

End of the Tutorial
-------------------

Well folks, that's all.

Even though Fenrir works, I actually use Clojure's default object system (the protocols, types & records one). It's very nice once you get to know it.
If, however, you feel that a class-based system is what your heart craves for, download Fenrir and play with it :-)

