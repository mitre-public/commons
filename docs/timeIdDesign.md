## TimeId Design Goals and Technical Thoughts

The package `org.mitre.caasd.commons.id` contains the `TimeId` and `SmallTimeId` classes.

### Bottom Line Up Front

1. Prefer using `TimeId`, it is simpler to use and more robust.
2. Only use `SmallTimeId` when saving 8-bytes per ID size is important.
3. Using `SmallTimeId` requires using a stateful `IdFactoryShard` to ensure the `SmallTimeIds` are unique.

### Design Goals

1. **Be unique**
2. **Be sortable by embedded timestamp**
3. **Allow retrieving the embedded timestamp**
4. **Enable compact serialization**

### Purposefully Designed to Inhibit

1. **Object Fingerprinting**: `TimeId` and `SmallTimeId` are NOT meant to encode the hash, checksum, or fingerprint of
   the item they will identify. Features that supported deterministically generating a `TimeId` or `SmallTimeId` from an
   object have been eliminated by design.

### Design Non-Goals

1. **Privacy**:  It is irrelevant if knowing one valid ID helps you guess a 2nd valid ID

### Diametrically Opposed Goals: Compact Serialization vs. Avoiding Hash Collisions

* You want an ID to use as few bytes as possible to reduce its size. ID size will be important for data structures and
  data storage layers that contain many IDs.
* You may also want UUID-like behavior where zero coordination is necessary to ensure IDs have an ultra-low chance of
  hash-collisions.
* Unfortunately, you CANNOT have both. These are diametrically opposed design goals.
    * Compact serialization requires most "bitsets in the set of all possible bitsets" to be allocated to a particular
      ID. If this weren't true, you'd shrink the number of bits used and save space.
    * Avoiding hash collisions requires most "bitsets in the set of all possible bitsets" to go unallocated. You get
      hash collisions if you don't meet this requirement.
* Consequently, we must CHOOSE ONE. Compact serialization and Avoiding Hash Collisions are oil and water.
    * `SmallTimeId` chooses compact serialization
    * `TimeId` chooses to avoid hash collisions

### Fact: `SmallTimeId's` 21 Bits is too small for Hashing

* The `SmallTimeId` implementation has **ONLY** 21-bits to distinguish different IDs that embed the same timestamp.
* This is a **VERY** small number of bits for avoiding hash-collisions. Only 2,097,152 different hashes exist.
* The probability of at least one hash-collisions inside a single "shared-timestamp bucket" is:
    * 5 items = 4.7683e-6
    * 10 items = 0.00002
    * 50 items = 0.00058
    * 100 items = 0.00236
    * 500 items = 0.05776
    * 1000 items = 0.21197
* Assigning the last 21-bits of a `SmallTimeId` with a hash-based method is fragile if not outright broken.
    * **Do not think:** "I have 2,097,152 hashing options, so I don't have to worry about collisions"
    * **Think:** "If I make 10 `SmallTimeId` referencing the same timestamp there is a 1 in 50,000" chance I'll have a
      collision. If I take this risk every millisecond I expect to see a hash collision every 50 seconds.
* **Due to this frailty, `SmallTimeId` should only be generated using a factory that implements a counting scheme.**

### Fact: `TimeId's` 86 Bits is enough for Hashing

* The `TimeId` implementation uses **86-bits** to distinguish different IDs that embed the same timestamp.
* This number of bits can ensure hash-collisions are virtually non-existent.
* For example, if you create 100 Million TimeId's that all embed the same timestamp (and thus have a chance to collide)
  then the probability of a hash collision is only 6.462e-11.
* TimeId's 86-bit pseudo-random bits permits 2^86 unique IDs for any single millisecond. This is
  77,371,252,455,336,267,181,195,264 possible hashes.

### Conclusion

* Using `SmallTimeId` safely essentially requires using a stateful `IdFactoryShard` to ensure the uniqueness of all
  new `SmallTimeIds`.
* Using Hashes to determine the 21-bits of a `SmallTimeId` is only appropriate when you only need to distinguish a
  small (e.g., <5) number of items per millisecond
* Hashing collisions are **very likely** if all the items in a dataset list the same Timestamp (e.g., exactly midnight)

### Idea Graveyard

* We decided to *not directly support* code that looks like `f(time, someObject) = an ID`.
    * Thus, this is **not facilitated:** `public <T> TimeId makeId(Instant time, T object)`
    * Thus, this is **not facilitated:** `public <T> SmallTimeId makeId(Instant time, T object)`
    * These usage patterns suggest the ID *should be* derived from T.
    * We disagree. An ID should be "buildable" without knowledge of T.
    * `TimeId` and `SmallTimeId` should not encode information about the object it is ID-ing.

* We considered, and rejected, supporting customizing how the 21 "counter bits" of `SmallTimeId` are set.
    * We did not add (actually, we *removed*) this flexibility because `SmallTimeId` is fragile. After all, you can only
      do so much with 21 bits. You cannot safely use 21 bits for a hash-based approach. The counter-based approach
      provided by `IdFactoryShard` may be the safest way to use the 21 bits to provide uniqueness.
    * Encouraging clients to use an `IdFactory` (by making it the easiest source of `SmallTimeIds`) helps ensure clients
      make IDs that are always unique. Opening the door to customized ID construction also opens the door to subtly
      broken IDs that will cause numerous heads.

* We considered, and rejected, adding support for customizing how the 86 pseudo-random bits of a `TimeId` are set.
    * We did not add this flexibility because `TimeId's` current implementation is robust.
    * Permitting customized ID construction erodes the "IDs are **always** unique" contract `TimeId` provides.
    * Permitting customized ID construction encourages clients to change what a `TimeId` "stands for" in a fundamentally
      unhelpful way. For example, `TimeId` should not have a dual-purpose Frankenstein role as both a *unique
      identifier* that can be assigned to a piece of data AND an *data fingerprint* that can help detect changes within
      that data.

### Roads left untraveled

* It is possible to use two "categorical variables" and a counter. For example, why not
  support: `42 bits | 8 bit-class | 5 bits-class | 8 counter-bits`
    * This could be the gateway to supporting `newSmallTimeId(time, enum1, enum2)`
* It is possible to reduce the number of bits allocated to storing the timestamp if you are willing to support a
  different universe of timestamps. For example, you could reject assigning timestamps for any time before 2000 (or some
  other date). 
