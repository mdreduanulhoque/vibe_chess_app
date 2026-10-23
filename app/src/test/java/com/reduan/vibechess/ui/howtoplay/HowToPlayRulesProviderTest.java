package com.reduan.vibechess.ui.howtoplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.reduan.vibechess.ui.game.BoardDemoConfig;

import org.junit.Test;

import java.util.List;

/**
 * Unit tests verifying the 18 visual rule sections and their {@link BoardDemoConfig}
 * illustrations in {@link HowToPlayRulesProvider}.
 */
public class HowToPlayRulesProviderTest {

    @Test
    public void getSections_returnsAll18RulesInOrderWithValidBoardConfigs() {
        List<HowToPlayRulesProvider.RuleSection> sections = HowToPlayRulesProvider.getSections();
        assertEquals(18, sections.size());

        for (int i = 0; i < sections.size(); i++) {
            HowToPlayRulesProvider.RuleSection section = sections.get(i);
            assertEquals(i + 1, section.sectionNumber);
            assertTrue(section.titleRes != 0);
            assertTrue(section.bodyRes != 0);
            assertNotNull(section.primaryBoard);

            int gridSize = section.primaryBoard.getGridSize();
            assertTrue(gridSize == 5 || gridSize == 8);
            assertTrue(section.primaryBoard.getBoard().points(com.reduan.vibechess.core.Color.WHITE) > 0);
        }

        // Section 2 is the 8x8 initial board setup (8*1 + 2*2 + 2*3 + 2*4 + 5 + 7 = 38 points)
        assertEquals(8, sections.get(1).primaryBoard.getGridSize());
        assertEquals(38, sections.get(1).primaryBoard.getBoard().points(com.reduan.vibechess.core.Color.WHITE));
        assertEquals(38, sections.get(1).primaryBoard.getBoard().points(com.reduan.vibechess.core.Color.BLACK));

        // Section 11 (En Passant) has both before and after 5x5 boards
        HowToPlayRulesProvider.RuleSection enPassant = sections.get(10);
        assertEquals(
                HowToPlayRulesProvider.ExtraVisualType.DUAL_BOARD_EN_PASSANT,
                enPassant.extraVisualType
        );
        assertNotNull(enPassant.secondaryBoard);
        assertEquals(5, enPassant.primaryBoard.getGridSize());
        assertEquals(5, enPassant.secondaryBoard.getGridSize());

        // Section 14 has the Points Table visual
        assertEquals(
                HowToPlayRulesProvider.ExtraVisualType.POINTS_TABLE,
                sections.get(13).extraVisualType
        );

        // Section 15 has Timer Badges visual
        assertEquals(
                HowToPlayRulesProvider.ExtraVisualType.TIMER_BADGES,
                sections.get(14).extraVisualType
        );

        // Section 18 has the Win Probability Bar visual
        assertEquals(
                HowToPlayRulesProvider.ExtraVisualType.PROBABILITY_BAR,
                sections.get(17).extraVisualType
        );
    }
}
