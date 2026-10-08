package com.ciphervault.app.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ciphervault.app.R;
import com.ciphervault.app.motion.NavigationMotionManager;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * Unit tests verifying Navigation Foundation architecture, graphs, and tab mappings (C).
 */
public class NavigationFoundationTest {

    @Test
    public void testStructuralDestinationFragmentInstantiable() {
        StructuralDestinationFragment fragment = new StructuralDestinationFragment();
        assertNotNull("StructuralDestinationFragment must be instantiable", fragment);
    }

    @Test
    public void testFourTopLevelTabMappings() {
        assertEquals("Home tab index must be 0", 0, NavigationMotionManager.TAB_INDEX_HOME);
        assertEquals("Files tab index must be 1", 1, NavigationMotionManager.TAB_INDEX_FILES);
        assertEquals("Activity tab index must be 2", 2, NavigationMotionManager.TAB_INDEX_ACTIVITY);
        assertEquals("Profile tab index must be 3", 3, NavigationMotionManager.TAB_INDEX_PROFILE);

        assertEquals(0, NavigationMotionManager.getTabIndexForMenuItemId(R.id.nav_home));
        assertEquals(1, NavigationMotionManager.getTabIndexForMenuItemId(R.id.nav_files));
        assertEquals(2, NavigationMotionManager.getTabIndexForMenuItemId(R.id.nav_activity));
        assertEquals(3, NavigationMotionManager.getTabIndexForMenuItemId(R.id.nav_profile));

        assertEquals(-1, NavigationMotionManager.getTabIndexForMenuItemId(-12345));
    }

    @Test
    public void testTabOrderingIsDeterministic() {
        Map<Integer, Integer> tabOrder = NavigationMotionManager.getTabOrderMap();
        assertEquals("Exactly four top-level tabs must be defined", 4, tabOrder.size());

        assertTrue(NavigationMotionManager.isTopLevelTab(R.id.nav_home));
        assertTrue(NavigationMotionManager.isTopLevelTab(R.id.nav_files));
        assertTrue(NavigationMotionManager.isTopLevelTab(R.id.nav_activity));
        assertTrue(NavigationMotionManager.isTopLevelTab(R.id.nav_profile));

        assertFalse(NavigationMotionManager.isTopLevelTab(999999));
    }

    @Test
    public void testTopLevelBackExitBehavior() {
        // All four top-level roots must trigger exit on back (Correction 1)
        assertTrue("Home root must exit on back", NavigationMotionManager.shouldExitOnBack(R.id.dest_home));
        assertTrue("Files root must exit on back", NavigationMotionManager.shouldExitOnBack(R.id.dest_files));
        assertTrue("Activity root must exit on back", NavigationMotionManager.shouldExitOnBack(R.id.dest_activity));
        assertTrue("Profile root must exit on back", NavigationMotionManager.shouldExitOnBack(R.id.dest_profile));

        // Child destinations must NOT exit on back
        assertFalse("Analytics must not exit on back", NavigationMotionManager.shouldExitOnBack(R.id.dest_analytics));
        assertFalse("Collections must not exit on back", NavigationMotionManager.shouldExitOnBack(R.id.dest_collections));
        assertFalse("Search must not exit on back", NavigationMotionManager.shouldExitOnBack(R.id.dest_search));
        assertFalse("Live Transfers must not exit on back", NavigationMotionManager.shouldExitOnBack(R.id.dest_live_transfers));
        assertFalse("Settings must not exit on back", NavigationMotionManager.shouldExitOnBack(R.id.dest_settings));
    }

    @Test
    public void testNavigationGraphResourcesExist() {
        File resNavDir = findResNavigationDirectory();
        assertNotNull("Navigation directory should be located", resNavDir);
        assertTrue("Navigation directory must exist", resNavDir.exists() && resNavDir.isDirectory());

        String[] requiredGraphs = {
                "nav_root.xml",
                "nav_main.xml",
                "nav_home.xml",
                "nav_files.xml",
                "nav_activity.xml",
                "nav_profile.xml"
        };

        for (String graphName : requiredGraphs) {
            File graphFile = new File(resNavDir, graphName);
            assertTrue("Graph file must exist: " + graphName, graphFile.exists());
            assertTrue("Graph file must not be empty: " + graphName, graphFile.length() > 0);
        }
    }

    @Test
    public void testNavigationGraphHierarchyDeclarations() throws IOException {
        File resNavDir = findResNavigationDirectory();
        if (resNavDir == null || !resNavDir.exists()) {
            return;
        }

        // nav_root.xml: contains Onboarding, Auth, nav_main
        String rootContent = new String(Files.readAllBytes(new File(resNavDir, "nav_root.xml").toPath()));
        assertTrue(rootContent.contains("dest_onboarding"));
        assertTrue(rootContent.contains("dest_auth"));
        assertTrue(rootContent.contains("@navigation/nav_main"));

        // nav_main.xml: contains nav_home, nav_files, nav_activity, nav_profile
        String mainContent = new String(Files.readAllBytes(new File(resNavDir, "nav_main.xml").toPath()));
        assertTrue(mainContent.contains("@navigation/nav_home"));
        assertTrue(mainContent.contains("@navigation/nav_files"));
        assertTrue(mainContent.contains("@navigation/nav_activity"));
        assertTrue(mainContent.contains("@navigation/nav_profile"));

        // nav_home.xml: contains home root and child destinations
        String homeContent = new String(Files.readAllBytes(new File(resNavDir, "nav_home.xml").toPath()));
        assertTrue(homeContent.contains("dest_home"));
        assertTrue(homeContent.contains("dest_analytics"));
        assertTrue(homeContent.contains("dest_collections"));
        assertTrue(homeContent.contains("dest_storage_category"));
        assertTrue(homeContent.contains("dest_security_status"));

        // nav_files.xml: contains files root and child destinations
        String filesContent = new String(Files.readAllBytes(new File(resNavDir, "nav_files.xml").toPath()));
        assertTrue(filesContent.contains("dest_files"));
        assertTrue(filesContent.contains("dest_search"));
        assertTrue(filesContent.contains("dest_upload_staging"));
        assertTrue(filesContent.contains("dest_camera_capture"));
        assertTrue(filesContent.contains("dest_large_files"));
        assertTrue(filesContent.contains("dest_file_viewer"));

        // nav_activity.xml: contains activity root and child destinations
        String activityContent = new String(Files.readAllBytes(new File(resNavDir, "nav_activity.xml").toPath()));
        assertTrue(activityContent.contains("dest_activity"));
        assertTrue(activityContent.contains("dest_live_transfers"));
        assertTrue(activityContent.contains("dest_audit_log"));

        // nav_profile.xml: contains profile root and child destinations
        String profileContent = new String(Files.readAllBytes(new File(resNavDir, "nav_profile.xml").toPath()));
        assertTrue(profileContent.contains("dest_profile"));
        assertTrue(profileContent.contains("dest_settings"));
        assertTrue(profileContent.contains("dest_account_management"));
        assertTrue(profileContent.contains("dest_diagnostics"));
    }

    private File findResNavigationDirectory() {
        File[] candidatePaths = {
                new File("src/main/res/navigation"),
                new File("app/src/main/res/navigation"),
                new File("../app/src/main/res/navigation")
        };
        for (File candidate : candidatePaths) {
            if (candidate.exists() && candidate.isDirectory()) {
                return candidate;
            }
        }
        return null;
    }
}
