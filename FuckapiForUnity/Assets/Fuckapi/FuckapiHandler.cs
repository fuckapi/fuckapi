using UnityEngine;
using System.Collections;
using UnityEngine.UI;


public class FuckapiHandler : MonoBehaviour
{
    public GameObject[] posMover;
    FuckapiObject[] fuckapiObjects;

    int objectNum;//オブジェクト数
    float nowPosf, targetPosf;
    Vector3[] nowPosV, targetPosV;
    public float nowSpeed;
    public float baseTimeLength;

    bool onMove;//移動中かどうか
    float startTime, nowTime;
    float journeyLength;
    
    Transform[] startRestriction, endRestriction;
    Vector3[] startRestrictionV, endRestrictionV;

    // Use this for initialization
    void Start()
    {
        //Fuckapiオブジェクトの取り込み
        objectNum = posMover.Length;

        fuckapiObjects = new FuckapiObject[objectNum];
        for (int i = 0; i < objectNum; i++)
        {
            //オブジェクト本体取り込み
            fuckapiObjects[i] = posMover[i].GetComponent<FuckapiObject>();
        }

        startRestriction = new Transform[objectNum];
        endRestriction = new Transform[objectNum];

        startRestrictionV = new Vector3[objectNum];
        endRestrictionV = new Vector3[objectNum];

        for (int i = 0; i < posMover.Length; i++)
        {
            //各種値等の取り込み
            startRestriction[i] = fuckapiObjects[i].getStart();
            endRestriction[i] = fuckapiObjects[i].getEnd();

            startRestrictionV[i] = startRestriction[i].position;
            endRestrictionV[i] = endRestriction[i].position;
        }

        nowPosV = new Vector3[objectNum];
        targetPosV = new Vector3[objectNum];
    }
    
    void Update()
    {
        if (onMove)
        {
            //動かす
            nowTime = Time.time - startTime;
            float rate = nowTime / journeyLength;
            for (int i = 0; i < posMover.Length; i++)
            {
                posMover[i].transform.position = Vector3.Lerp(nowPosV[i], targetPosV[i], rate);
            }

            if (rate >= 1.0f)
            {
                //目標地点まで行ったら終わり
                onMove = false;
            }
        }
    }

    public void ReceiveMessage(string message)
    {
        string[] param = message.Split(',');
        Vector3 vectorResult = new Vector3(float.Parse(param[0]), float.Parse(param[1]), float.Parse(param[2]));

        int rate = (int)(vectorResult[2]) / 10;

        if (rate == 0)
        {
            // 微動には反応させない
        }
        else if (rate > -3 && rate < 3)
        {
            targetPosf = targetPosf - (vectorResult[2] / 100);
            if (targetPosf < 0) targetPosf = 0;
            if (targetPosf > 1) targetPosf = 1;
        }
        else
        {
            targetPosf = targetPosf - (vectorResult[2] / 1000);
            if (targetPosf < 0) targetPosf = 0;
            if (targetPosf > 1) targetPosf = 1;
        }

        //受け取ったバリューに基づき目標地点を計算
        for (int i = 0; i < objectNum; i++)
        {
            targetPosV[i] = startRestrictionV[i] + targetPosf * (endRestrictionV[i] - startRestrictionV[i]);
            nowPosV[i] = posMover[i].transform.position;
        }

        startTime = Time.time;
        journeyLength = baseTimeLength / nowSpeed;
        onMove = true;
    }
}
