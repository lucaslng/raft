package com.lucaslng.raft.physics;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

// Used by Bullet physics engine to sync physics world and outside world
public class MotionState extends btMotionState {

	private final Matrix4 transform;
	private Matrix4 temp;
	private final float HALF_HEIGHT;

	public MotionState(Matrix4 transform, float halfHeight) {
		super();
		this.transform = transform;
		this.temp = new Matrix4();
		this.HALF_HEIGHT = halfHeight;
	}
	
	@Override
	public void getWorldTransform(Matrix4 worldTrans) {
		temp.set(transform);
    temp.val[Matrix4.M13] += HALF_HEIGHT;
    worldTrans.set(temp);
	}

	@Override
	public void setWorldTransform(Matrix4 worldTrans) {
		temp.set(worldTrans);
		temp.val[Matrix4.M13] -= HALF_HEIGHT;
		transform.set(temp);
	}
}