/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2014.                            (c) 2014.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *  $Revision: 4 $
 *
 ************************************************************************
 */
package ca.nrc.cadc.ac.server.ldap;

import java.security.AccessControlException;
import java.security.Principal;
import java.util.Collection;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;

import ca.nrc.cadc.ac.User;
import ca.nrc.cadc.ac.UserAlreadyExistsException;
import ca.nrc.cadc.ac.UserNotFoundException;
import ca.nrc.cadc.ac.UserRequest;
import ca.nrc.cadc.ac.server.UserPersistence;
import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.profiler.Profiler;

public class LdapUserPersistence extends LdapPersistence implements UserPersistence
{
    private static final Logger logger = Logger.getLogger(LdapUserPersistence.class);

    public LdapUserPersistence()
    {
        super();
    }

    /**
     * Shutdown the connection pool.
     */
    @Override
    public void destroy()
    {
        super.shutdown();
    }

    /**
     * Add the user to the users tree.
     *
     * @param user      The user request to put into the user tree.
     *
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     * @throws ca.nrc.cadc.ac.UserAlreadyExistsException
     */
    public void addUser(User user)
        throws TransientException, AccessControlException, UserAlreadyExistsException
    {
        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            userDAO.addUser(user);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Add the user to the user requests tree.
     *
     * @param userRequest      The user request to put into the pending user tree.
     *
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     * @throws ca.nrc.cadc.ac.UserAlreadyExistsException
     */
    public void addUserRequest(UserRequest userRequest)
        throws TransientException, AccessControlException, UserAlreadyExistsException
    {
        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            userDAO.addUserRequest(userRequest);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Get the user specified by userID from the active users tree.
     *
     * @param userID The userID.
     *
     * @return User instance.
     *
     * @throws UserNotFoundException when the user is not found.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public User getUser(Principal userID)
        throws UserNotFoundException, TransientException, AccessControlException
    {
        Subject caller = AuthenticationUtil.getCurrentSubject();
        if ( !isMatch(caller, userID) )
            throw new AccessControlException("permission denied: target user does not match current user");

        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            return userDAO.getUser(userID);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Get the user specified by email address exists in the active users tree.
     *
     * @param emailAddress The user's email address.
     *
     * @return User ID.
     *
     * @throws UserNotFoundException when the user is not found.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     * @throws UserAlreadyExistsException A user with the same email address already exists
     */
    public User getUserByEmailAddress(String emailAddress)
        throws UserNotFoundException, TransientException,
               AccessControlException, UserAlreadyExistsException
        {
            LdapConnections conns = new LdapConnections(this);
            try
            {
                LdapUserDAO userDAO = new LdapUserDAO(conns);
                return userDAO.getUserByEmailAddress(emailAddress);
            }
            finally
            {
                conns.releaseConnections();
            }
        }

    /**
    * Get the user specified by userID whose account is pending approval.
    *
    * @param userID The userID.
    * @return User instance.
    * @throws UserNotFoundException  when the user is not found.
    * @throws TransientException     If an temporary, unexpected problem occurred.
    * @throws AccessControlException If the operation is not permitted.
    */
    public User getUserRequest(Principal userID)
        throws UserNotFoundException, TransientException, AccessControlException
    {
        Subject caller = AuthenticationUtil.getCurrentSubject();
        if ( !isMatch(caller, userID) )
            throw new AccessControlException("permission denied: target user does not match current user");

        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            return userDAO.getUserRequest(userID);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Get the user specified by userID with all of the users identities.
     *
     * @param userID The userID.
     *
     * @return User instance.
     *
     * @throws UserNotFoundException when the user is not found.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public User getAugmentedUser(Principal userID)
        throws UserNotFoundException, TransientException
    {
        // internal call to return user identities: no permission check
        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            Profiler profiler = new Profiler(LdapUserPersistence.class);
            userDAO = new LdapUserDAO(conns);
            profiler.checkpoint("Create LdapUserDAO");
            User user = userDAO.getAugmentedUser(userID);
            profiler.checkpoint("getAugmentedUser");
            return user;
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Get all user names from the active users tree.
     *
     * @return A collection of strings.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public Collection<User> getUsers()
        throws TransientException, AccessControlException
    {
        // current policy: usernames visible to all authenticated users
        Subject caller = AuthenticationUtil.getCurrentSubject();
        if (caller == null || AuthMethod.ANON.equals(AuthenticationUtil.getAuthMethod(caller)))
            throw new AccessControlException("Caller is not authenticated");

        // user must also have an approved account
        if (caller.getPrincipals(HttpPrincipal.class).isEmpty())
            throw new AccessControlException("Caller does not have authorized account");

        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            return userDAO.getUsers();
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Get all user names from the user requests tree.
     *
     * @return A collection of strings.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public Collection<User> getUserRequests()
        throws TransientException, AccessControlException
    {
        // admin API: no permission check
        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            return userDAO.getUserRequests();
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Move the user request specified by userID from the
     * user requests tree to the users tree.
     *
     * @param userID      The user instance to move.
     *
     * @return User instance.
     *
     * @throws UserNotFoundException when the user is not found.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public User approveUserRequest(Principal userID)
        throws UserNotFoundException, TransientException,
        AccessControlException
    {
        // admin API: no permission check
        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            return userDAO.approveUserRequest(userID);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Updated the user specified by userID in the active users tree.
     *
     * @param user          The user to update.
     *
     * @return User instance.
     *
     * @throws UserNotFoundException when the user is not found.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public User modifyUser(User user)
        throws UserNotFoundException, TransientException,
        AccessControlException
    {
        Subject caller = AuthenticationUtil.getCurrentSubject();
        if ( !isMatch(caller, user) )
            throw new AccessControlException("permission denied: target user does not match current user");

        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            return userDAO.modifyUser(user);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Delete the user specified by userID.
     *
     * @param userID The userID.
     *
     * @throws UserNotFoundException when the user is not found.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public void deactivateUser(Principal userID)
        throws UserNotFoundException, TransientException,
        AccessControlException
    {
        Subject caller = AuthenticationUtil.getCurrentSubject();
        if ( !isMatch(caller, userID) )
            throw new AccessControlException("permission denied: target user does not match current user");

        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            userDAO.deleteUser(userID, true);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Delete the user specified by userID.
     *
     * @param userID The userID.
     *
     * @throws UserNotFoundException when the user is not found.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public void deleteUser(Principal userID)
        throws UserNotFoundException, TransientException,
        AccessControlException
    {

        // admin API: permission checks done in action layer
        // and in ACIs.
        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            userDAO.deleteUser(userID, false);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Delete the user specified by userID from the user requests tree.
     *
     * @param userID The userID.
     *
     * @throws UserNotFoundException when the user is not found.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public void deleteUserRequest(Principal userID)
        throws UserNotFoundException, TransientException,
        AccessControlException
    {
        // admin API: no permission check
        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            userDAO.deleteUserRequest(userID);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Get the user specified by userID.
     *
     * @param userID The userID.
     *
     * @return Boolean.
     *
     * @throws UserNotFoundException when the user is not found.
     * @throws TransientException If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public Boolean doLogin(String userID, String password)
        throws UserNotFoundException, TransientException, AccessControlException
    {
        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            return userDAO.doLogin(userID, password);
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Update a user's password. The given user and authenticating user must match.
     *
     * @param userID        the user.
     * @param oldPassword   current password.
     * @param newPassword   new password.
     * @throws UserNotFoundException If the given user does not exist.
     * @throws TransientException   If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public void setPassword(HttpPrincipal userID, String oldPassword, String newPassword)
        throws UserNotFoundException, TransientException, AccessControlException
    {
        Subject caller = AuthenticationUtil.getCurrentSubject();
        if ( !isMatch(caller, userID) )
            throw new AccessControlException("permission denied: target user does not match current user");

        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            if (userDAO.doLogin(userID.getName(), oldPassword))
            {
                // oldPassword is correct
                userDAO.setPassword(userID, oldPassword, newPassword);
            }
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    /**
     * Reset a user's password. The given user and authenticating user must match.
     *
     * @param userID        The user.
     * @param newPassword   new password.
     * @throws UserNotFoundException If the given user does not exist.
     * @throws TransientException   If an temporary, unexpected problem occurred.
     * @throws AccessControlException If the operation is not permitted.
     */
    public void resetPassword(HttpPrincipal userID, String newPassword)
        throws UserNotFoundException, TransientException, AccessControlException
    {
        Subject caller = AuthenticationUtil.getCurrentSubject();
        if ( !isMatch(caller, userID) )
            throw new AccessControlException("permission denied: target user does not match current user");

        LdapUserDAO userDAO = null;
        LdapConnections conns = new LdapConnections(this);
        try
        {
            userDAO = new LdapUserDAO(conns);
            User user = getUser(userID);

            if (user != null)
            {
                // oldPassword is correct
                userDAO.resetPassword(userID, newPassword);
            }
        }
        finally
        {
            conns.releaseConnections();
        }
    }

    private boolean isMatch(Subject caller, User user)
    {
        if (caller == null || AuthMethod.ANON.equals(AuthenticationUtil.getAuthMethod(caller)))
            throw new AccessControlException("Caller is not authenticated");

        for (Principal pc : caller.getPrincipals())
        {
            for (Principal pu : user.getIdentities())
            {
                if (AuthenticationUtil.equals(pc, pu))
                    return true;
            }
        }
        return false;
    }

    private boolean isMatch(Subject caller, Principal identity)
    {
        if (caller == null || AuthMethod.ANON.equals(AuthenticationUtil.getAuthMethod(caller)))
            throw new AccessControlException("Caller is not authenticated");

        for (Principal pc : caller.getPrincipals())
        {
            if (AuthenticationUtil.equals(pc, identity))
                return true;
        }
        return false;
    }

}
