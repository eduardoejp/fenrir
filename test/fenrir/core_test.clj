(ns fenrir.core-test
  (:require [clojure.test :refer :all]
            [fenrir :refer :all]))

;;;

(defclass fShape [] []
  (get area -1)
  (get perimeter -1))

(defclass fSquare [fShape] [side]
  (get area (* side side))
  (get perimeter (* side 4)))

(defclass fCircle [fShape] [radius]
  (get diameter (* radius 2))
  (get area (* Math/PI (* radius radius)))
  (get perimeter (* Math/PI (get-slot *self* :diameter))))

(def s (ctor fShape))
(def sq (ctor fSquare :side 5))
(def cir (ctor fCircle :radius 6))

(deftest shape-tests
  (testing "shape"
    (are [x] (= x -1)
      (get-slot s :area)
      (get-slot s :perimeter)))
  (testing "square"
    (is (= 5 (get-slot sq :side)))
    (is (= 25 (get-slot sq :area)))
    (is (= 20 (get-slot sq :perimeter))))
  (testing "circle"
    (is (= 6 (get-slot cir :radius)))
    (is (= 12 (get-slot cir :diameter)))
    (is (= (* Math/PI 36) (get-slot cir :area)))
    (is (= (* Math/PI 12) (get-slot cir :perimeter)))))

;;;

(defclass fPet [] [name age owner]
  "A pet of any kind."
  (make-noise [] "Make some pet noise."))

(defclass fDog [fPet] []
  "A dog of any race."
  (set age (if (< *val* 15) *val* -1)) ; If the dog is too old, put it to sleep...
  (get name (str owner "'s dog, " name))
  (make-noise [] (println "Bark!")))

(defclass fMachine [] [name manufacturer]
  "A machine of some kind."
  (get name (str name ", Copyright (C) " manufacturer " 2015"))
  (explode [] (println "BOOM!")))

(defclass fRoboDog [fDog fMachine] [name]
  "fDog 2.0"
  (ctor [name age owner manufacturer]
    (base-ctor *fclass* :name name :age age :owner owner :manufacturer manufacturer))
  (make-noise [] (println "*Buzz* Bark! *Buzz*"))
  (mask name fMachine))

(def pet (ctor fPet :name "Claws" :age 5 :owner "Mr. Foo"))
(def dog (ctor fDog :name "Rex" :age 5 :owner "Mr. Bar"))
(def machine (ctor fMachine :name "IPhone" :manufacturer "Apple Inc."))
(def robodog (ctor fRoboDog "TRex" 10 "Ms. Baz" "Bots R Us"))

(deftest robopet-tests
  (testing "pet"
    (is (= "Make some pet noise." (make-noise pet))))
  (testing "dog"
    (is (= "Bark!\n" (with-out-str (make-noise dog))))
    (is (= "Mr. Bar's dog, Rex" (get-slot dog :name)))
    (let [pup (set-slot dog :age 3)]
      (is (= 3 (get-slot pup :age))))
    (let [too-old (set-slot dog :age 18)]
      (is (= -1 (get-slot too-old :age)))))
  (testing "machine"
    (is (= "Apple Inc." (get-slot machine :manufacturer)))
    (is (= "IPhone, Copyright (C) Apple Inc. 2015" (get-slot machine :name))))
  (testing "robodog"
    (is (= "*Buzz* Bark! *Buzz*\n" (with-out-str (make-noise robodog))))
    (is (= "TRex, Copyright (C) Bots R Us 2015" (get-slot robodog :name)))))

;;;

(ns chess
  (:require [fenrir :refer :all]))

(defclass Piece [] [rank]
  (pawn? "Returns whether or not this piece is a pawn."))

(in-ns 'fenrir.core-test)

(defclass Color [] [color]
  (get color (str "the color " color)))

(defclass Knight [chess/Piece Color] []
  (pawn? [] false))

(def knight (ctor Knight :color "black"))

(deftest namespace-tests
  (testing "a class inheriting from a class in another namespace"
    (is (= false (pawn? knight)))
    (is (= "the color black" (get-slot knight :color)))))
