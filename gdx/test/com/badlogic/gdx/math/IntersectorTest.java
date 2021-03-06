
package com.badlogic.gdx.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.badlogic.gdx.math.Intersector.MinimumTranslationVector;
import com.badlogic.gdx.math.Intersector.SplitTriangle;

public class IntersectorTest {

	/** Compares two triangles for equality. Triangles must have the same winding, but may begin with different vertex. Values are
	 * epsilon compared, with default tolerance. Triangles are assumed to be valid triangles - no duplicate vertices. */
	private static boolean triangleEquals (float[] base, int baseOffset, int stride, float[] comp) {
		assertTrue(stride >= 3);
		assertTrue(base.length - baseOffset >= 9);
		assertTrue(comp.length == 9);

		int offset = -1;
		// Find first comp vertex in base triangle
		for (int i = 0; i < 3; i++) {
			int b = baseOffset + i * stride;
			if (MathUtils.isEqual(base[b], comp[0]) && MathUtils.isEqual(base[b + 1], comp[1])
				&& MathUtils.isEqual(base[b + 2], comp[2])) {
				offset = i;
				break;
			}
		}
		assertTrue("Triangles do not have common first vertex.", offset != -1);
		// Compare vertices
		for (int i = 0; i < 3; i++) {
			int b = baseOffset + ((offset + i) * stride) % (3 * stride);
			int c = i * stride;
			if (!MathUtils.isEqual(base[b], comp[c]) || !MathUtils.isEqual(base[b + 1], comp[c + 1])
				|| !MathUtils.isEqual(base[b + 2], comp[c + 2])) {
				return false;
			}
		}
		return true;
	}

	@Test
	public void testSplitTriangle () {
		Plane plane = new Plane(new Vector3(1, 0, 0), 0);
		SplitTriangle split = new SplitTriangle(3);

		{// All back
			float[] fTriangle = {-10, 0, 10, -1, 0, 0, -12, 0, 10}; // Whole triangle on the back side
			Intersector.splitTriangle(fTriangle, plane, split);
			assertTrue(split.numBack == 1);
			assertTrue(split.numFront == 0);
			assertTrue(split.total == 1);
			assertTrue(triangleEquals(split.back, 0, 3, fTriangle));

			fTriangle[4] = 5f;
			assertFalse("Test is broken", triangleEquals(split.back, 0, 3, fTriangle));
		}

		{// All front
			float[] fTriangle = {10, 0, 10, 1, 0, 0, 12, 0, 10}; // Whole triangle on the front side
			Intersector.splitTriangle(fTriangle, plane, split);
			assertTrue(split.numBack == 0);
			assertTrue(split.numFront == 1);
			assertTrue(split.total == 1);
			assertTrue(triangleEquals(split.front, 0, 3, fTriangle));
		}

		{// Two back, one front
			float[] triangle = {-10, 0, 10, 10, 0, 0, -10, 0, -10}; // ABC One vertex in front, two in back
			Intersector.splitTriangle(triangle, plane, split); // Split points are D (0,0,5) and E (0,0,-5)
			assertTrue(split.numBack == 2);
			assertTrue(split.numFront == 1);
			assertTrue(split.total == 3);
			// There is only one way to triangulate front
			assertTrue(triangleEquals(split.front, 0, 3, new float[] {0, 0, 5, 10, 0, 0, 0, 0, -5}));

			// There are two ways to triangulate back
			float[][] firstWay = { {-10, 0, 10, 0, 0, 5, 0, 0, -5}, {-10, 0, 10, 0, 0, -5, -10, 0, -10}};// ADE AEC
			float[][] secondWay = { {-10, 0, 10, 0, 0, 5, -10, 0, -10}, {0, 0, 5, 0, 0, -5, -10, 0, -10}};// ADC DEC
			float[] base = split.back;
			boolean first = (triangleEquals(base, 0, 3, firstWay[0]) && triangleEquals(base, 9, 3, firstWay[1]))
				|| (triangleEquals(base, 0, 3, firstWay[1]) && triangleEquals(base, 9, 3, firstWay[0]));
			boolean second = (triangleEquals(base, 0, 3, secondWay[0]) && triangleEquals(base, 9, 3, secondWay[1]))
				|| (triangleEquals(base, 0, 3, secondWay[1]) && triangleEquals(base, 9, 3, secondWay[0]));
			assertTrue("Either first or second way must be right (first: " + first + ", second: " + second + ")", first ^ second);
		}

		{// Two front, one back
			float[] triangle = {10, 0, 10, -10, 0, 0, 10, 0, -10}; // ABC One vertex in back, two in front
			Intersector.splitTriangle(triangle, plane, split); // Split points are D (0,0,5) and E (0,0,-5)
			assertTrue(split.numBack == 1);
			assertTrue(split.numFront == 2);
			assertTrue(split.total == 3);
			// There is only one way to triangulate back
			assertTrue(triangleEquals(split.back, 0, 3, new float[] {0, 0, 5, -10, 0, 0, 0, 0, -5}));

			// There are two ways to triangulate front
			float[][] firstWay = { {10, 0, 10, 0, 0, 5, 0, 0, -5}, {10, 0, 10, 0, 0, -5, 10, 0, -10}};// ADE AEC
			float[][] secondWay = { {10, 0, 10, 0, 0, 5, 10, 0, -10}, {0, 0, 5, 0, 0, -5, 10, 0, -10}};// ADC DEC
			float[] base = split.front;
			boolean first = (triangleEquals(base, 0, 3, firstWay[0]) && triangleEquals(base, 9, 3, firstWay[1]))
				|| (triangleEquals(base, 0, 3, firstWay[1]) && triangleEquals(base, 9, 3, firstWay[0]));
			boolean second = (triangleEquals(base, 0, 3, secondWay[0]) && triangleEquals(base, 9, 3, secondWay[1]))
				|| (triangleEquals(base, 0, 3, secondWay[1]) && triangleEquals(base, 9, 3, secondWay[0]));
			assertTrue("Either first or second way must be right (first: " + first + ", second: " + second + ")", first ^ second);
		}
	}

	@Test
	public void testOverlapConvexPolygons () {		
		/*
		 * Rough sketch:         depth
		 *                      |-----|
		 * D##########################C
		 * #                          #
		 * #                    1 ° ° #
		 * #                    °     #° °
		 * #                    °     #    ° °
		 * #                    °     #        ° °
		 * #                    °     #           ° 3
		 * #                     °    #        ° °
		 * #                     °    #    ° °
		 * #                     °    #° °
		 * #                     2 ° °#
		 * #                          #
		 * A##########################B
		 * 
		 */ 
		
		/*
		 * When the triangle is checked against the rectangle, the MTV should have the normal (1.0, 0.0).
		 * 
		 * Instead it has the normal (-1.0, 0.0) because the triangle has more points on the left side of the checked axis (normal
		 * of edge B-C) of the rectangle than on the right. This results in the triangle moving further into the rectangle if the
		 * MTV is applied to its position.
		 */
		
		MinimumTranslationVector mtv = new MinimumTranslationVector();
		float triangleMinX = 14;
		float squareMaxX = 16;
		float expectedDepth = Math.abs(squareMaxX - triangleMinX);
		
		// Given a rectangle and a slightly irregular triangle overlapping from the right
		float[] triangle = {triangleMinX, 4, triangleMinX + 0.1f, 12, 22, 8};
		float[] rectangle = {0, 0, squareMaxX, 0, squareMaxX, 16, 0, 16};

		// When check for overlap
		boolean overlaps = Intersector.overlapConvexPolygons(triangle, rectangle, mtv);

		// Assert that it resolves the collision by translating to the right
		assertTrue("They should overlap", overlaps);
		assertEquals("MTV depth should be ~" + expectedDepth, expectedDepth, mtv.depth, 0.01f);
		assertEquals("MTV shouldn't translate on the y-axis", 0f, mtv.normal.y, 0f);
		assertTrue("MTV should translate to the right", mtv.normal.x > 0f);
	}

}
