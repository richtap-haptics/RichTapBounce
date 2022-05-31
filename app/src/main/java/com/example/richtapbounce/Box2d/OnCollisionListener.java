package com.example.richtapbounce.Box2d;

/**
 * Alerts you to collisions between views within the layout
 */
public interface OnCollisionListener {
    /**
     * Called when a collision is entered between two bodies. ViewId can also be
     * R.id.physics_bound_top,
     * R.id.physics_bound_bottom,
     * R.id.physics_bound_left, or
     * R.id.physics_bound_right.
     * If view was not assigned an id, the return value will be [View.NO_ID].
     *
     * @param viewIdA view id of body A
     * @param viewIdB view id of body B
     */
    void onCollisionEntered(int viewIdA, int viewIdB);

    /**
     * Called when a collision is exited between two bodies. ViewId can also be
     * R.id.physics_bound_top,
     * R.id.physics_bound_bottom,
     * R.id.physics_bound_left, or
     * R.id.physics_bound_right.
     * If view was not assigned an id, the return value will be [View.NO_ID].
     *
     * @param viewIdA view id of body A
     * @param viewIdB view id of body B
     */
    void onCollisionExited(int viewIdA, int viewIdB);
}
