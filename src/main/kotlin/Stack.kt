// copied and simplified from
// https://github.com/gazolla/Kotlin-Algorithm/tree/master/Stack
// kittekat Oct/2017

package net.kittenberger.wolfgang.jack2c

class Stack<T>() {
  private var items: ArrayList<T> = arrayListOf() 

  fun isEmpty():Boolean = this.items.isEmpty()

  fun count():Int = this.items.count()

  fun push(element:T) {
    val position = this.count()
    this.items.add(position, element)
  }

  override  fun toString() = this.items.toString()

  fun pop():T? {
    if (this.isEmpty()) {
      return null
    } else {
      val item =  this.items.count() - 1
      return this.items.removeAt(item)
    }
  }

  fun peek():T? {
    if (isEmpty()) {
      return null
    } else {
      return this.items[this.items.count() - 1]
    }
  }
}
