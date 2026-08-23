#!/usr/bin/env python3
"""Independent Python reconstruction/geometry check against the parent v0.2.3 fixture.

The actual Java StimulusCatalog is tested separately by JUnit against all 289 fixture rows.
"""
from __future__ import annotations
import csv, math
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FIXTURE=ROOT/'fixtures'/'parent_manifest.csv'
LEFT=[(266.,102.),(626.,102.),(266.,366.),(626.,366.),(357.,176.),(718.,176.),(357.,440.),(718.,440.)]
W=[-100.,-100.,-100.,-100.,100.,100.,100.,100.]
SCALES=[0.10,0.25,0.50,0.75,1.0,1.25,1.50,1.75,2.0,2.5,3.0,3.5,4.0,4.5,5.0,5.5,6.0,6.5]
LOCAL=[(0.,0.),(5.,5.),(10.,10.),(15.,0.),(20.,5.),(-5.,-5.),(5.,-5.),(-10.,10.)]
PARENT_SHA='51cf147a8c8cf8016795cfcccefa63cdb058612bb3fcb8f5166eface8ebaec52'

def generated_catalog():
    out=[]
    n=1
    out.append((f'S{n:04d}',0.0,1,0.0,0.0)); n+=1
    for sign in (1,-1):
        for g in SCALES:
            for v4,v6 in LOCAL:
                out.append((f'S{n:04d}',g,sign,v4,v6)); n+=1
    return out

def fixture_catalog():
    with FIXTURE.open(newline='',encoding='utf-8') as f:
        return [(
            r['stimulus_id'],float(r['global_scale']),int(float(r['sign'])),
            float(r['vertex4_dx_model_units']),float(r['vertex6_dx_model_units'])
        ) for r in csv.DictReader(f)]

def main():
    expected=fixture_catalog(); got=generated_catalog()
    assert len(expected)==289, len(expected)
    assert len(got)==289, len(got)
    for i,(a,b) in enumerate(zip(expected,got),start=1):
        assert a[0]==b[0], (i,a,b)
        assert a[2]==b[2], (i,a,b)
        for j in (1,3,4):
            assert abs(a[j]-b[j])<1e-12, (i,a,b)

    # Independent g=1, positive, unperturbed geometry fixture.
    a=math.radians(6.0)
    base=[x*math.cos(a)+w*math.sin(a)-x for (x,_),w in zip(LEFT,W)]
    d=[0.52*x for x in base]
    rms=math.sqrt(sum(x*x for x in d)/8)
    mx=max(abs(x) for x in d)
    assert abs(rms-5.510951125589413)<1e-12, rms
    assert abs(mx-7.218712709637384)<1e-12, mx
    assert got[0]==('S0001',0.0,1,0.0,0.0)
    print(f'Independent Python reconstruction matches 289 fixture rows; g=1 RMS={rms:.12f}, max={mx:.12f}')
    print('Parent full manifest SHA-256:',PARENT_SHA)

if __name__=='__main__': main()
