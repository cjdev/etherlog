package org.teamstory.http

import org.httpobjects._

class GenericWrapper(val w:HttpObject, val decorator:(String, Request, (Request)=>Response)=>Response) extends HttpObject(w.pattern().raw()) {
  override def get(req:Request) = decorator("get", req, w.get)
  override def post(req:Request) = decorator("post", req, w.post)
  override def put(req:Request) = decorator("put", req, w.put)
  override def delete(req:Request) = decorator("delete", req, w.delete)
}
