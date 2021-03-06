package zio.cache

/**
 * A `Priority` is used to determine which values that may potentially be
 * retained in the cache should be retained if there is not sufficient space
 * in the cache for all values that could potentially be retained. Unlike
 * `Evict`, which describes a binary distinction between values that must be
 * evicted from the cache and values that may potentially be retained,
 * `Priority` describes a relative ordering of values that may potentially be
 * retained.
 *
 * You can think of `Priority` as like an `Ordering` except that the relative
 * priority of two entries in the cache may depend on the current time as well
 * as the two entries.
 */
sealed abstract class Priority[-Value] { self =>

  def compare(left: Entry[Value], right: Entry[Value]): CacheWorth

  /**
   * A symbolic alias for `andThen`.
   */
  final def ++[Value1 <: Value](that: Priority[Value1]): Priority[Value1] =
    self andThen that

  /**
   * Composes this `Priority` with the specified `Priority` to return a new
   * `Priority` that first prioritizes values based on `self`, and if two
   * values have equal priority based on `self` then prioritizing them based
   * on `that`.
   */
  final def andThen[Value1 <: Value](that: Priority[Value1]): Priority[Value1] =
    Priority((left, right) => self.compare(left, right) ++ that.compare(left, right))

  /**
   * Returns a new `Priority` that is the inverse of this `Priority`, so the
   * highest priority value in this `Priority` would have the lowest priority
   * in the new `Priority` and vice versa.
   */
  final def flip: Priority[Value] =
    Priority((left, right) => compare(left, right).flip)

  final def toOrdering[Value1 <: Value]: Ordering[Entry[Value1]] =
    new Ordering[Entry[Value1]] {
      def compare(l: Entry[Value1], r: Entry[Value1]): Int =
        self.compare(l, r) match {
          case CacheWorth.Left  => -1
          case CacheWorth.Equal => 0
          case CacheWorth.Right => 1
        }
    }
}

object Priority {

  /**
   * Constructs a `Priority` from a function that computes a relative ranking
   * given the current time and two entries.
   */
  def apply[Value](compare0: (Entry[Value], Entry[Value]) => CacheWorth) =
    new Priority[Value] {
      def compare(left: Entry[Value], right: Entry[Value]): CacheWorth =
        compare0(left, right)
    }

  /**
   * A `Priority` that considers all cache entries to have equal priority.
   */
  val any: Priority[Any] =
    Priority((_, _) => CacheWorth.Equal)

  /**
   * Constructs a `Priority` from a function that projects the cache or entry
   * statistics to a value for which an `Ordering` is defined.
   */
  def fromOrdering[A](proj: Entry[Any] => A)(implicit ord: Ordering[A]): Priority[Any] =
    Priority { (left, right) =>
      val compare = ord.compare(proj(left), proj(right))
      if (compare < 0) CacheWorth.Left
      else if (compare > 0) CacheWorth.Right
      else CacheWorth.Equal
    }

  /**
   * Constructs a `Priority` from an entry value for which an `Ordering` is
   * defined.
   */
  def fromOrderingValue[A](implicit ord: Ordering[A]): Priority[A] =
    Priority { (left, right) =>
      val compare = ord.compare(left.value, right.value)
      if (compare < 0) CacheWorth.Left
      else if (compare > 0) CacheWorth.Right
      else CacheWorth.Equal
    }
}
