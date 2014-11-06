package org.teamstory.datas

trait DatabaseTrait[T] {
    def get(id:String)(implicit manifest:Manifest[T]):T
    def put(id:String, data:T):Unit
    def contains(id:String): Boolean
    def toStream(implicit manifest:Manifest[T]):Stream[T]
    def map[R](fn:(String, T)=>R)(implicit manifest:Manifest[T]):Seq[R]
    def scan(fn:(String, T)=>Unit)(implicit manifest:Manifest[T])
    def filter(fn:(String, T)=>Boolean)(implicit manifest:Manifest[T]):Stream[T]
}
