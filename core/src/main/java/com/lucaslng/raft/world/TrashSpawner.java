package com.lucaslng.raft.world;

import java.util.LinkedList;
import java.util.Queue;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.lucaslng.raft.entity.OceanTrash;

public class TrashSpawner {

	private Vector2 direction;
	private float chanceToSpawn = .01f;
	private Queue<OceanTrash> trash = new LinkedList<>();

	public TrashSpawner() {
		direction = new Vector2(0f, 1f);
		
	}

	public void step(float delta, Camera camera) {
		if (!trash.isEmpty()) {
			OceanTrash element = trash.element();
			// if too far from camera, despawn
		}

		if (Math.random() < chanceToSpawn) {
			// pick position along axis
			trash.add(new OceanTrash());
		}
	}
	
}
