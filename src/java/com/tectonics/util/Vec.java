package com.tectonics.util;

import java.awt.Point;
import java.awt.Graphics;
import java.awt.Color;

public class Vec {

    public static final Vec ZERO   = new Vec(0f, 0f);
    public static final Vec UNIT_X = new Vec(1f, 0f);
    public static final Vec UNIT_Y = new Vec(0f, 1f);

    public final float x;
    public final float y;

    public Vec(final float x, final float y) {
        this.x = x;
        this.y = y;
    }


    public float len() {
        return (float) Math.sqrt(Vec.dot(this, this));
    }

    public Vec normal() {
        return Vec.scale(this, 1f / len());
    }

    public Vec negate() {
        return Vec.scale(this, -1f);
    }

    public Vec rotate(final float radians) {
        return new Vec(
                (float) (Math.cos(radians) * x - Math.sin(radians) * y),
                (float) (Math.sin(radians) * x + Math.cos(radians) * y)
            );
    }

    public Vec independent() {
        return rotate((float) (Math.PI / 2.0));
    }

    public Point truncate() {
        return new Point((int) x, (int) y);
    }

    public Point truncateTowardsZero() {
        return new Point(2 * Math.round(0.5f * x), 2 * Math.round(0.5f * y));
    }

    public void paint(final Graphics g, final Color color, final float scale, final Point position) {
        final Color prevColor = g.getColor();
        g.setColor(color);

        final Vec scaled = scale(this, scale);
        final Point head = Util.sumPoints(position, scaled.truncate());
        final Vec headBase = Vec.scale(scaled.negate(), 0.3f);
        final Point leftHead = Util.sumPoints(head, headBase.rotate((float) (Math.PI / 6.0)).truncate());
        final Point rightHead = Util.sumPoints(head, headBase.rotate((float) (- Math.PI / 6.0)).truncate());

        g.drawLine(position.x, position.y, head.x, head.y);
        g.drawLine(head.x, head.y, leftHead.x, leftHead.y);
        g.drawLine(head.x, head.y, rightHead.x, rightHead.y);

        g.setColor(prevColor);
    }

    @Override
    public String toString() {
        return "Vec(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vec) {
            final Vec v = (Vec) obj;
            return x == v.x && y == v.y;
        }
        return false;
    }




    public static Vec fromAngle(final float radians) {
        return new Vec((float) Math.cos(radians), (float) Math.sin(radians));
    }

    public static Vec randomDirection(final float magnitude) {
        return Vec.scale(Vec.fromAngle((float) (Math.random() * Math.PI * 2.0)), magnitude);
    }

    public static Vec extend(final Point p) {
        return new Vec(p.x, p.y);
    }

    public static Vec scale(final Vec v, final float scalar) {
        return new Vec(v.x * scalar, v.y * scalar);
    }

    public static float dot(final Vec v0, final Vec v1) {
        return v0.x * v1.x + v0.y * v1.y;
    }

    public static Vec sum(final Vec...vs) {
        float x = 0f;
        float y = 0f;

        for (final Vec v : vs) {
            x += v.x;
            y += v.y;
        }

        return new Vec(x, y);
    }


    public static float project(final Vec v, final Vec onto) {
        if (onto.equals(Vec.ZERO)) return 0f;
        return Vec.dot(v, onto.normal());
    }
}
