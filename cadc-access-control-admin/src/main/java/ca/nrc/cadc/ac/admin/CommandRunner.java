/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2015.                            (c) 2015.
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
 *
 ************************************************************************
 */

package ca.nrc.cadc.ac.admin;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;

import ca.nrc.cadc.ac.UserNotFoundException;
import ca.nrc.cadc.ac.server.UserPersistence;
import ca.nrc.cadc.ac.server.ldap.LdapConfig;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.DelegationToken;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.PrincipalExtractor;
import ca.nrc.cadc.auth.SSOCookieCredential;
import ca.nrc.cadc.auth.X509CertificateChain;
import ca.nrc.cadc.net.TransientException;


public class CommandRunner
{
    private final static Logger LOGGER = Logger.getLogger(CommandRunner.class);
    private final CmdLineParser commandLineParser;
    private final UserPersistence userPersistence;


    public CommandRunner(final CmdLineParser commandLineParser,
                         final UserPersistence userPersistence)
    {
        this.commandLineParser = commandLineParser;
        this.userPersistence = userPersistence;
    }


    /**
     * Run a suitable action command.
     *
     */
    public void run() throws UserNotFoundException, TransientException
    {
        AbstractCommand command = commandLineParser.getCommand();
        command.setUserPersistence(userPersistence);

        Principal userIDPrincipal = null;
        if (command instanceof AbstractUserCommand)
        {
            userIDPrincipal = ((AbstractUserCommand) command).getPrincipal();
        }

        if (userIDPrincipal == null)
        {
            // run as the operator
            LdapConfig config = LdapConfig.getLdapConfig();
            String proxyDN = config.getProxyUserDN();
            if (proxyDN == null)
                throw new IllegalArgumentException("No ldap account in .dbrc");

            String userIDLabel = "uid=";
            int uidIndex = proxyDN.indexOf("uid=");
            int commaIndex = proxyDN.indexOf(",", userIDLabel.length());
            String userID = proxyDN.substring(uidIndex + userIDLabel.length(), commaIndex);
            userIDPrincipal = new HttpPrincipal(userID);
        }

        // run as the user
        LOGGER.debug("running as " + userIDPrincipal.getName());
        Set<Principal> userPrincipals = new HashSet<Principal>(1);
        userPrincipals.add(userIDPrincipal);
        AnonPrincipalExtractor principalExtractor = new AnonPrincipalExtractor(userPrincipals);
        Subject subject = AuthenticationUtil.getSubject(principalExtractor);
        Subject.doAs(subject, command);
    }

    class AnonPrincipalExtractor implements PrincipalExtractor
    {
        Set<Principal> principals;

        AnonPrincipalExtractor(Set<Principal> principals)
        {
            this.principals = principals;
        }
        public Set<Principal> getPrincipals()
        {
            return principals;
        }
        public X509CertificateChain getCertificateChain()
        {
            return null;
        }
        public DelegationToken getDelegationToken()
        {
            return null;
        }
        public SSOCookieCredential getSSOCookieCredential()
        {
            return null;
        }
    }
}