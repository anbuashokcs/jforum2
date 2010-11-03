/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * This file creation date: 19/03/2004 - 18:45:54
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic.security;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import net.jforum.JForumExecutionContext;
import net.jforum.exceptions.DatabaseException;
import net.jforum.security.PermissionControl;
import net.jforum.security.Role;
import net.jforum.security.RoleCollection;
import net.jforum.security.RoleValue;
import net.jforum.security.RoleValueCollection;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.lang.StringUtils;

/**
 * @author Rafael Steil
 * @version $Id: SecurityCommon.java,v 1.13 2007/08/16 13:07:34 rafaelsteil Exp $
 */
public class SecurityCommon
{
	/**
	 * Execute the <i>add role</i> thing. As the SQL statement to insert user and group roles are
	 * different, they cannot be manipulated with a 'generic' statement, and is for this reason that
	 * <code>addRole</code> method is marked abstract. <br>
	 * The only job the <code>addRole</code> method should do is to get the correct SQL statement
	 * for each case - user or group - and then re-pass it to this method, who then do the job for us.
	 * 
	 * @param sql The SQL statement to be executed.
	 * @param id The ID do insert. May be user's or group's id, depending of the situation ( the caller )
	 * @param role The role name to insert
	 * @param roleValues A <code>RoleValueCollection</code> collection containing the role values to
	 * insert. If none is wanted, just pass null as argument.
	 * @param supportAutoGeneratedKeys Set to <code>true</code> if <i>Statement.RETURN_GENERATED_KEYS</i> is supported
	 * by the Driver, or <code>false</code> if not.
	 * @param autoKeysQuery String
	 */
	public static void executeAddRole(String sql, int id, Role role, RoleValueCollection roleValues,
			boolean supportAutoGeneratedKeys, String autoKeysQuery)
	{
		PreparedStatement p = null;
		ResultSet rs = null;
		
		try {
			if (supportAutoGeneratedKeys) {
				p = JForumExecutionContext.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			}
			else {
				p = JForumExecutionContext.getConnection().prepareStatement(sql);
			}

			p.setInt(1, id);
			p.setString(2, role.getName());

			p.executeUpdate();

			if (roleValues != null) {
				int roleId = -1;

				if (supportAutoGeneratedKeys) {
					rs = p.getGeneratedKeys();
					rs.next();
					roleId = rs.getInt(1);
				}
				else {
					p = JForumExecutionContext.getConnection().prepareStatement(autoKeysQuery);
					rs = p.executeQuery();
					if (rs.next()) {
						roleId = rs.getInt(1);
					}
				}
				rs.close();
				p.close();

				if (roleId == -1) {
					throw new SQLException("Could not obtain the latest role id");
				}

				p = JForumExecutionContext.getConnection().prepareStatement(
						SystemGlobals.getSql("PermissionControl.addRoleValues"));

				for (Iterator<?> iter = roleValues.iterator(); iter.hasNext();) {
					RoleValue rv = (RoleValue) iter.next();

					p.setInt(1, roleId);
					p.setString(2, rv.getValue());

					p.executeUpdate();
				}
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, p);
		}
	}

	/**
	 * See {@link PermissionControl#addRole(int, net.jforum.security.Role, net.jforum.security.RoleValueCollection)} for
	 * explanation about this method. The working way is the same.
	 * 
	 * @param rs The ResultSet containing the data to be fetched. This method does not
	 * free the resultset after it finished using it, so it's responsibility of the 
	 * caller to do such task.
	 * @return A <code>RoleCollection</code> collection with the roles processed.
	 */
	public static RoleCollection loadRoles(ResultSet rs)
	{
		RoleCollection rc = new RoleCollection();

		try {
			Role r = null;
			String lastName = null;

			while (rs.next()) {
				String currentName = rs.getString("name");
				
				if (!currentName.equals(lastName)) {
					if (r != null) {
						rc.add(r);
					}

					r = new Role();
					r.setName(rs.getString("name"));

					lastName = currentName;
				}

				String roleValue = rs.getString("role_value");

				if (!rs.wasNull() && StringUtils.isNotBlank(roleValue)) {
					r.getValues().add(new RoleValue(roleValue));
				}
			}

			if (r != null) {
				rc.add(r);
			}

			return rc;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}
	
	public static String groupIdAsString(int[] ids)
	{
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < ids.length - 1; i++) {
			sb.append(ids[i]).append(',');
		}
		
		if (ids.length > 0) {
			sb.append(ids[ids.length - 1]);			
		}
		
		return sb.toString();
	}
}
