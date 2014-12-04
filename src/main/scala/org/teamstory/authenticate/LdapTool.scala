package org.teamstory.authenticate

import java.util.Hashtable
import javax.naming.Context
import javax.naming.ldap.Control
import javax.naming.ldap.InitialLdapContext
import javax.naming.AuthenticationException
import javax.naming.NamingException
import javax.naming.directory.SearchControls
import javax.naming.NamingEnumeration
import javax.naming.directory.SearchResult

class LdapTool(url: String, ldapUser: String, ldapPassword: String) extends AuthMechanism {
  val connCtls = Array[Control]()

  def authenticate(name: String, password: String, ctx: InitialLdapContext = connect()) = {
    try {
      ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, name)
      ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password)
      ctx.reconnect(connCtls)
      true
    } catch {
      case e: AuthenticationException => false
      case e: NamingException => false
    }
  }

  def findByEmail(email: String, ctx: InitialLdapContext) = {
    val controls = new SearchControls() {
      setReturningObjFlag(true)
      setSearchScope(SearchControls.SUBTREE_SCOPE)
    }

    val filter = "(&(objectclass=user)(mail=" + email + "))"
    val answer = ctx.
      search("", filter, controls).
      asInstanceOf[NamingEnumeration[SearchResult]]

    if (answer.hasMore) {
      Some(answer.next())
    } else {
      None
    }

  }

  override def emailExists(email: String) = {
    val ctx = connect()
    authenticate(ldapUser, ldapPassword, ctx)
    findByEmail(email, ctx) match {
      case Some(user) => true
      case None => false
    }
  }

  override def authenticateEmail(email: String, password: String) = {
    val ctx = connect()
    authenticate(ldapUser, ldapPassword, ctx)
    findByEmail(email, ctx) match {
      case Some(user) =>
        val passwordIsGood = authenticate(user.getName, password)
        if (passwordIsGood) {
          // user
          Some(AuthDetailsPlaceholder)
        } else {
          None
        }
      case None => None
    }
  }

  def connect() = {
    val env = new Hashtable[String, String]() {
      put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
      put(Context.SECURITY_AUTHENTICATION, "simple")
      put(Context.PROVIDER_URL, url)
    }

    new InitialLdapContext(env, connCtls)
  }
}