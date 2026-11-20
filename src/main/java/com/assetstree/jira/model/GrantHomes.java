package com.assetstree.jira.model;

/**
 * A grant that was stored for every project is kept only on the projects it
 * already belonged to. A named group stays where that group is saved. A person
 * without a group stays where Jira already lets them open the project.
 */
public final class GrantHomes {
    private GrantHomes() {
    }

    public static boolean stays(boolean forUser, boolean roleNamed, boolean roleLivesHere, boolean opensInJira) {
        if (roleNamed) {
            return roleLivesHere;
        }
        return forUser && opensInJira;
    }
}
