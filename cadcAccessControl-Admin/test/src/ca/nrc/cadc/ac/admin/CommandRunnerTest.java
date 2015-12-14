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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ca.nrc.cadc.ac.User;
import ca.nrc.cadc.ac.server.UserPersistence;
import ca.nrc.cadc.auth.HttpPrincipal;


@SuppressWarnings("unchecked")
public class CommandRunnerTest
{
    final CmdLineParser mockParser = createMock(CmdLineParser.class);
    final UserPersistence mockUserPersistence =
            createMock(UserPersistence.class);

    public CommandRunnerTest()
    {
        // Set the necessary JNDI system property for lookups.
        System.setProperty("java.naming.factory.initial",  ContextFactoryImpl.class.getName());
    }


    @Test
    public void listUsers() throws Exception
    {
        final CommandRunner testSubject =
                new CommandRunner(mockParser, mockUserPersistence);
        final List<User<Principal>> userData = new ArrayList<>();
        final Principal p = new HttpPrincipal("TEST USER");

        userData.add(new User<>(p));

        expect(mockParser.getCommand()).andReturn(new ListActiveUsers());

        expect(mockUserPersistence.getUsers()).andReturn(userData).once();
        replay(mockParser, mockUserPersistence);

        testSubject.run();

        verify(mockParser, mockUserPersistence);
    }

    @Test
    public void listPendingUsers() throws Exception
    {
        final CommandRunner testSubject =
                new CommandRunner(mockParser, mockUserPersistence);
        final List<User<HttpPrincipal>> userData = new ArrayList<>();

        userData.add(new User<>(new HttpPrincipal("PENDING USER")));

        expect(mockParser.getCommand()).andReturn(new ListPendingUsers());

        expect(mockUserPersistence.getPendingUsers()).andReturn(userData).once();
        replay(mockParser, mockUserPersistence);

        testSubject.run();

        verify(mockParser, mockUserPersistence);
    }

    @Test
    public void viewUser() throws Exception
    {
        final CommandRunner testSubject =
                new CommandRunner(mockParser, mockUserPersistence);
        final HttpPrincipal principalData = new HttpPrincipal("TESTUSER");
        final User<HttpPrincipal> userData = new User<>(principalData);

        expect(mockParser.getCommand()).andReturn(new ViewUser("TESTUSER"));

        expect(mockUserPersistence.getUser(principalData)).
                andReturn(userData).once();
        replay(mockParser, mockUserPersistence);

        testSubject.run();

        verify(mockParser, mockUserPersistence);
    }

    @Test
    public void approveUser() throws Exception
    {
        final CommandRunner testSubject =
                new CommandRunner(mockParser, mockUserPersistence);
        final HttpPrincipal principalData = new HttpPrincipal("PENDINGUSER");
        final User<HttpPrincipal> userData = new User<>(principalData);

        expect(mockParser.getCommand()).andReturn(new ApproveUser("PENDINGUSER", "CN=DN"));

        expect(mockUserPersistence.approvePendingUser(principalData)).andReturn(userData).once();
        expect(mockUserPersistence.getUser(principalData)).andReturn(userData).once();
        expect(mockUserPersistence.modifyUser(userData)).andReturn(null).once();
        replay(mockParser, mockUserPersistence);

        testSubject.run();

        verify(mockParser, mockUserPersistence);
    }

    @Test
    public void rejectUser() throws Exception
    {
        final CommandRunner testSubject =
                new CommandRunner(mockParser, mockUserPersistence);
        final HttpPrincipal principalData = new HttpPrincipal("PENDINGUSER");

        expect(mockParser.getCommand()).andReturn(
                new RejectUser("PENDINGUSER"));

        mockUserPersistence.deletePendingUser(principalData);
        expectLastCall().once();

        replay(mockParser, mockUserPersistence);

        testSubject.run();

        verify(mockParser, mockUserPersistence);
    }
}