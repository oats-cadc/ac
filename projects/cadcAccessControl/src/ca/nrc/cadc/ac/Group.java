/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2014.                            (c) 2014.
 * National Research Council            Conseil national de recherches
 * Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 * All rights reserved                  Tous droits reserves
 *
 * NRC disclaims any warranties         Le CNRC denie toute garantie
 * expressed, implied, or statu-        enoncee, implicite ou legale,
 * tory, of any kind with respect       de quelque nature que se soit,
 * to the software, including           concernant le logiciel, y com-
 * without limitation any war-          pris sans restriction toute
 * ranty of merchantability or          garantie de valeur marchande
 * fitness for a particular pur-        ou de pertinence pour un usage
 * pose.  NRC shall not be liable       particulier.  Le CNRC ne
 * in any event for any damages,        pourra en aucun cas etre tenu
 * whether direct or indirect,          responsable de tout dommage,
 * special or general, consequen-       direct ou indirect, particul-
 * tial or incidental, arising          ier ou general, accessoire ou
 * from the use of the software.        fortuit, resultant de l'utili-
 *                                      sation du logiciel.
 *
 *
 * @author adriand
 * 
 * @version $Revision: $
 * 
 * 
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.ac;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

public class Group
{
    private String groupID;

    private User<? extends Principal> owner;

    // group's properties
    protected Set<GroupProperty> properties = new HashSet<GroupProperty>();

    // group's user members
    private Set<User<? extends Principal>> userMembers = 
            new HashSet<User<? extends Principal>>();
    // group's group members
    private Set<Group> groupMembers = new HashSet<Group>();

    public String description;
    
    // Access Control properties
    /**
     * group that can read details of this group
     * Note: this class does not enforce any access control rules
     */
    public Group groupRead;
    /**
     * group that can read and write details of this group
     * Note: this class does not enforce any access control rules
     */
    public Group groupWrite;
    /**
     * flag that show whether the details of this group are publicly readable
     * Note: this class does not enforce any access control rules
     */
    public boolean publicRead = false;

    /**
     * Ctor.
     * 
     * @param groupID
     *            Unique ID for the group. Must be a valid URI fragment component,
     *            so it's restricted to alphanumeric and "-", ".","_","~" characters.
     * @param owner
     *            Owner/Creator of the group.
     */
    public Group(final String groupID,
            final User<? extends Principal> owner)
    {
        if(groupID == null)
        {
            throw new IllegalArgumentException("Null groupID");
        }
        
        // check for invalid path characters in groupID
        if(!groupID.matches("^[a-zA-Z0-9\\-\\.~_]*$"))
            throw new IllegalArgumentException("Invalid group ID " + groupID
                    + ": may not contain space ( ), slash (/), escape (\\), or percent (%)");

        this.groupID = groupID;
        if(owner == null)
        {
            throw new IllegalArgumentException("Null owner");
        }
        this.owner = owner;
    }

    /**
     * Obtain this Group's unique id.
     * 
     * @return String group ID.
     */
    public String getID()
    {
        return groupID;
    }

    /**
     * Obtain this group's owner
     * @return owner of the group
     */
    public User<? extends Principal> getOwner()
    {
        return owner;
    }

    /**
     * 
     * @return a set of properties associated with a group
     */
    public Set<GroupProperty> getProperties()
    {
        return properties;
    }

    /**
     * 
     * @return individual user members of this group
     */
    public Set<User<? extends Principal>> getUserMembers()
    {
        return userMembers;
    }

    /**
     * 
     * @return group members of this group
     */
    public Set<Group> getGroupMembers()
    {
        return groupMembers;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return 31  + groupID.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof Group))
        {
            return false;
        }
        Group other = (Group) obj;
        if (!groupID.equals(other.groupID))
        {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "[" + groupID + "]";
    }

}
