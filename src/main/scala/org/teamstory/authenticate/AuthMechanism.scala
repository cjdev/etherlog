package org.teamstory.authenticate

trait AuthMechanism {
    def authenticateEmail(email:String, password:String):Option[AuthDetails]
    def emailExists(email:String):Boolean
}

trait AuthDetails

//can't have case classes with no members, so this will have to do for now
case object AuthDetailsPlaceholder extends AuthDetails
