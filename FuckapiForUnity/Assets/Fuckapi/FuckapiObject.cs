using UnityEngine;
using System.Collections;

public class FuckapiObject : MonoBehaviour {
    public Transform startTransform, endTransform;
    public float speed;
	// Use this for initialization
	void Start () {
        
	}
	
	// Update is called once per frame
	void Update () {
	
	}

    public Transform getStart()
    {
        return startTransform;
    }

    public Transform getEnd()
    {
        return endTransform;
    }

    public float getSpeed()
    {
        return speed;
    }
}
