import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const asset = path.join(root, 'src/main/resources/assets/nemesis_ai');
for (const dir of ['geo', 'animations', 'textures/entity', 'models']) fs.mkdirSync(path.join(asset, dir), {recursive:true});

const cube = (origin, size, uv, inflate=0) => ({origin,size,uv,inflate});
const bones = [
 {name:'root',pivot:[0,0,0],children:[
  {name:'body',pivot:[0,22,0],rotation:[-8,0,0],cubes:[cube([-6,18,-3],[12,14,7],[0,0],0.3)],children:[
   {name:'chest_core',pivot:[0,26,-4],cubes:[cube([-3,23,-5],[6,7,2],[40,0],0.35)]},
   {name:'neck',pivot:[0,31,-1],rotation:[22,0,0],cubes:[cube([-3,29,-3],[6,7,6],[56,0])],children:[
    {name:'head',pivot:[0,35,-1],rotation:[-15,0,0],cubes:[cube([-6,32,-8],[12,9,14],[0,28],0.5),cube([-5,35,5],[10,6,9],[52,26],0.25)],children:[
     {name:'jaw',pivot:[0,34,-7],cubes:[cube([-5,31,-10],[10,3,11],[0,54]),cube([-4,30,-10],[2,2,9],[44,54]),cube([2,30,-10],[2,2,9],[44,54])],children:[
      {name:'inner_jaw',pivot:[0,32,-8],cubes:[cube([-2,31,-15],[4,2,8],[70,54]),cube([-1.5,30,-16],[3,1,3],[86,54])]}
     ]}
    ]}
   ]},
   {name:'back_spikes',pivot:[0,28,3],cubes:[cube([-1,28,3],[2,8,3],[96,0]),cube([-1,23,3],[2,6,3],[106,0]),cube([-1,19,3],[2,5,3],[116,0])]},
   {name:'right_arm',pivot:[-6,29,0],rotation:[5,0,8],cubes:[cube([-10,18,-2],[4,12,4],[0,72])],children:[{name:'right_forearm',pivot:[-8,19,0],rotation:[-18,0,0],cubes:[cube([-11,8,-2],[5,11,5],[20,72])],children:[{name:'right_hand',pivot:[-8,9,0],cubes:[cube([-11,5,-3],[6,5,6],[42,72]),cube([-11,0,-3],[1,7,1],[68,72]),cube([-8,0,-3],[1,7,1],[72,72]),cube([-5,0,-3],[1,7,1],[76,72])]}]}]},
   {name:'left_arm',pivot:[6,29,0],rotation:[5,0,-8],cubes:[cube([6,18,-2],[4,12,4],[0,72])],mirror:true,children:[{name:'left_forearm',pivot:[8,19,0],rotation:[-18,0,0],cubes:[cube([6,8,-2],[5,11,5],[20,72])],mirror:true,children:[{name:'left_hand',pivot:[8,9,0],cubes:[cube([5,5,-3],[6,5,6],[42,72]),cube([10,0,-3],[1,7,1],[68,72]),cube([7,0,-3],[1,7,1],[72,72]),cube([4,0,-3],[1,7,1],[76,72])],mirror:true}]}]},
   {name:'right_leg',pivot:[-3,20,0],rotation:[-12,0,2],cubes:[cube([-6,10,-2],[6,11,6],[0,92])],children:[{name:'right_shin',pivot:[-3,11,0],rotation:[18,0,0],cubes:[cube([-5,2,-1],[5,10,5],[28,92])],children:[{name:'right_foot',pivot:[-3,3,-1],cubes:[cube([-6,0,-7],[6,4,9],[50,92]),cube([-6,0,-11],[1,2,5],[80,92]),cube([-3.5,0,-11],[1,2,5],[84,92]),cube([-1,0,-11],[1,2,5],[88,92])]}]}]},
   {name:'left_leg',pivot:[3,20,0],rotation:[-12,0,-2],cubes:[cube([0,10,-2],[6,11,6],[0,92])],mirror:true,children:[{name:'left_shin',pivot:[3,11,0],rotation:[18,0,0],cubes:[cube([0,2,-1],[5,10,5],[28,92])],mirror:true,children:[{name:'left_foot',pivot:[3,3,-1],cubes:[cube([0,0,-7],[6,4,9],[50,92]),cube([5,0,-11],[1,2,5],[80,92]),cube([2.5,0,-11],[1,2,5],[84,92]),cube([0,0,-11],[1,2,5],[88,92])],mirror:true}]}]},
   {name:'tail_1',pivot:[0,20,3],rotation:[-15,0,0],cubes:[cube([-3,16,3],[6,6,9],[96,32])],children:[
    {name:'tail_2',pivot:[0,19,11],rotation:[0,18,0],cubes:[cube([-2.5,16,10],[5,5,9],[96,50])],children:[
     {name:'tail_3',pivot:[0,18,18],rotation:[0,22,0],cubes:[cube([-2,16,17],[4,4,9],[96,66])],children:[
      {name:'tail_4',pivot:[0,18,25],rotation:[0,25,0],cubes:[cube([-1.5,16.5,24],[3,3,9],[96,80])],children:[{name:'tail_tip',pivot:[0,18,32],rotation:[0,28,0],cubes:[cube([-1,17,31],[2,2,10],[96,92])]}]}
     ]}
    ]}
   ]}
  ]}
 ]}
];

const geoBones=[];
function flattenGeo(source, parent=null) {
 for (const bone of source) {
  const {children, ...flat}=bone;
  if (parent) flat.parent=parent;
  geoBones.push(flat);
  if (children) flattenGeo(children, bone.name);
 }
}
flattenGeo(bones);
const geo={format_version:'1.12.0','minecraft:geometry':[{description:{identifier:'geometry.nemesis',texture_width:128,texture_height:128,visible_bounds_width:4.5,visible_bounds_height:3.4,visible_bounds_offset:[0,1.35,0]},bones:geoBones}]};

const k=(time,v)=>({[time]:{vector:v,lerp_mode:'catmullrom'}});
const merge=(...o)=>Object.assign({},...o);
const rot=(...pairs)=>Object.fromEntries(pairs.map(([t,v])=>[String(t),{vector:v,lerp_mode:'catmullrom'}]));
const loop=(len,b)=>({loop:true,animation_length:len,bones:b});
const once=(len,b)=>({loop:false,animation_length:len,bones:b});
const animations={format_version:'1.8.0',animations:{
 'animation.nemesis.idle':loop(3,{body:{rotation:rot([0,[-8,0,0]],[1.5,[-5,0,0]],[3,[-8,0,0]])},head:{rotation:rot([0,[-15,-3,0]],[1.5,[-12,4,0]],[3,[-15,-3,0]])},chest_core:{scale:rot([0,[1,1,1]],[1.5,[1.08,1.12,1.08]],[3,[1,1,1]])},tail_1:{rotation:rot([0,[-15,-8,0]],[1.5,[-15,10,0]],[3,[-15,-8,0]])},tail_2:{rotation:rot([0,[0,-10,0]],[1.5,[0,12,0]],[3,[0,-10,0]])},tail_3:{rotation:rot([0,[0,-8,0]],[1.5,[0,15,0]],[3,[0,-8,0]])}}),
 'animation.nemesis.walk':loop(1,{body:{position:rot([0,[0,0,0]],[.25,[0,-1,0]],[.5,[0,0,0]],[.75,[0,-1,0]],[1,[0,0,0]]),rotation:rot([0,[-8,0,-3]],[.25,[-6,0,0]],[.5,[-8,0,3]],[.75,[-6,0,0]],[1,[-8,0,-3]])},right_leg:{rotation:rot([0,[34,0,2]],[.25,[2,0,0]],[.5,[-34,0,-2]],[.75,[0,0,0]],[1,[34,0,2]])},left_leg:{rotation:rot([0,[-34,0,-2]],[.25,[0,0,0]],[.5,[34,0,2]],[.75,[2,0,0]],[1,[-34,0,-2]])},right_shin:{rotation:rot([0,[12,0,0]],[.25,[48,0,0]],[.5,[18,0,0]],[.75,[8,0,0]],[1,[12,0,0]])},left_shin:{rotation:rot([0,[18,0,0]],[.25,[8,0,0]],[.5,[12,0,0]],[.75,[48,0,0]],[1,[18,0,0]])},right_foot:{rotation:rot([0,[-16,0,0]],[.25,[-30,0,0]],[.5,[12,0,0]],[.75,[4,0,0]],[1,[-16,0,0]])},left_foot:{rotation:rot([0,[12,0,0]],[.25,[4,0,0]],[.5,[-16,0,0]],[.75,[-30,0,0]],[1,[12,0,0]])},right_arm:{rotation:rot([0,[-25,0,8]],[.5,[28,0,8]],[1,[-25,0,8]])},left_arm:{rotation:rot([0,[28,0,-8]],[.5,[-25,0,-8]],[1,[28,0,-8]])},tail_1:{rotation:rot([0,[-15,-12,0]],[.5,[-15,12,0]],[1,[-15,-12,0]])}}),
 'animation.nemesis.run':loop(.6,{body:{position:rot([0,[0,0,0]],[.15,[0,-1.5,0]],[.3,[0,0,0]],[.45,[0,-1.5,0]],[.6,[0,0,0]]),rotation:rot([0,[-22,0,-4]],[.3,[-18,0,4]],[.6,[-22,0,-4]])},right_leg:{rotation:rot([0,[52,0,2]],[.3,[-52,0,-2]],[.6,[52,0,2]])},left_leg:{rotation:rot([0,[-52,0,-2]],[.3,[52,0,2]],[.6,[-52,0,-2]])},right_shin:{rotation:rot([0,[8,0,0]],[.15,[62,0,0]],[.3,[22,0,0]],[.45,[5,0,0]],[.6,[8,0,0]])},left_shin:{rotation:rot([0,[22,0,0]],[.15,[5,0,0]],[.3,[8,0,0]],[.45,[62,0,0]],[.6,[22,0,0]])},right_foot:{rotation:rot([0,[-24,0,0]],[.15,[-38,0,0]],[.3,[18,0,0]],[.6,[-24,0,0]])},left_foot:{rotation:rot([0,[18,0,0]],[.3,[-24,0,0]],[.45,[-38,0,0]],[.6,[18,0,0]])},right_arm:{rotation:rot([0,[-42,0,8]],[.3,[42,0,8]],[.6,[-42,0,8]])},left_arm:{rotation:rot([0,[42,0,-8]],[.3,[-42,0,-8]],[.6,[42,0,-8]])}}),
 'animation.nemesis.claw_attack':once(.7,{body:{rotation:rot([0,[-8,0,0]],[.25,[-12,-18,0]],[.5,[-10,24,0]],[.7,[-8,0,0]])},right_arm:{rotation:rot([0,[5,0,8]],[.25,[-70,-20,35]],[.5,[50,20,-40]],[.7,[5,0,8]])},right_forearm:{rotation:rot([0,[-18,0,0]],[.25,[-55,0,0]],[.5,[15,0,0]],[.7,[-18,0,0]])}}),
 'animation.nemesis.bite':once(.8,{body:{position:rot([0,[0,0,0]],[.35,[0,0,-4]],[.55,[0,0,2]],[.8,[0,0,0]])},head:{rotation:rot([0,[-15,0,0]],[.3,[-35,0,0]],[.55,[12,0,0]],[.8,[-15,0,0]])},jaw:{rotation:rot([0,[0,0,0]],[.3,[38,0,0]],[.55,[4,0,0]],[.8,[0,0,0]])},inner_jaw:{position:rot([0,[0,0,0]],[.35,[0,0,-7]],[.6,[0,0,0]],[.8,[0,0,0]])}}),
 'animation.nemesis.hurt':once(.45,{body:{rotation:rot([0,[-8,0,0]],[.12,[-2,0,12]],[.3,[-14,0,-7]],[.45,[-8,0,0]])},head:{rotation:rot([0,[-15,0,0]],[.12,[-28,12,0]],[.45,[-15,0,0]])}}),
 'animation.nemesis.death':once(1.8,{root:{rotation:rot([0,[0,0,0]],[.7,[0,0,38]],[1.35,[0,0,88]],[1.8,[0,0,90]]),position:rot([0,[0,0,0]],[1.35,[0,-17,0]],[1.8,[0,-17,0]])},head:{rotation:rot([0,[-15,0,0]],[1.2,[-35,0,15]],[1.8,[-35,0,15]])}}),
 'animation.nemesis.adapt_melee':once(1.4,{body:{rotation:rot([0,[-8,0,0]],[.45,[-18,0,0]],[1,[-18,0,0]],[1.4,[-8,0,0]])},right_arm:{rotation:rot([0,[5,0,8]],[.45,[-65,-20,35]],[1,[-65,-20,35]],[1.4,[5,0,8]])},left_arm:{rotation:rot([0,[5,0,-8]],[.45,[-65,20,-35]],[1,[-65,20,-35]],[1.4,[5,0,-8]])}}),
 'animation.nemesis.adapt_ranged':once(1.4,{body:{rotation:rot([0,[-8,0,0]],[.35,[-28,15,0]],[1,[-28,-15,0]],[1.4,[-8,0,0]])},right_arm:{rotation:rot([0,[5,0,8]],[.35,[-105,-20,65]],[1,[-105,-20,65]],[1.4,[5,0,8]])},left_arm:{rotation:rot([0,[5,0,-8]],[.35,[-105,20,-65]],[1,[-105,20,-65]],[1.4,[5,0,-8]])},head:{rotation:rot([0,[-15,0,0]],[.35,[-42,0,0]],[1,[-42,0,0]],[1.4,[-15,0,0]])}})
}};

const elements=[];
function bbGroup(b) {
 const gu=crypto.randomUUID();
 const group={name:b.name,origin:b.pivot??[0,0,0],rotation:b.rotation??[0,0,0],color:0,uuid:gu,export:true,isOpen:true,locked:false,visibility:true,autouv:0,children:[]};
 for (const c of b.cubes??[]) {
  const u=crypto.randomUUID(), from=c.origin, to=c.origin.map((n,i)=>n+c.size[i]);
  elements.push({name:`${b.name}_cube`,box_uv:true,rescale:false,locked:false,from,to,autouv:0,color:0,origin:b.pivot??[0,0,0],rotation:[0,0,0],faces:{north:{uv:[c.uv[0],c.uv[1],c.uv[0]+c.size[0],c.uv[1]+c.size[1]],texture:0},east:{uv:[c.uv[0],c.uv[1],c.uv[0]+c.size[2],c.uv[1]+c.size[1]],texture:0},south:{uv:[c.uv[0],c.uv[1],c.uv[0]+c.size[0],c.uv[1]+c.size[1]],texture:0},west:{uv:[c.uv[0],c.uv[1],c.uv[0]+c.size[2],c.uv[1]+c.size[1]],texture:0},up:{uv:[c.uv[0],c.uv[1],c.uv[0]+c.size[0],c.uv[1]+c.size[2]],texture:0},down:{uv:[c.uv[0],c.uv[1],c.uv[0]+c.size[0],c.uv[1]+c.size[2]],texture:0}},type:'cube',uuid:u,inflate:c.inflate??0,mirror_uv:!!b.mirror});
  group.children.push(u);
 }
 for (const child of b.children??[]) group.children.push(bbGroup(child));
 return group;
}
const outliner=bones.map(bbGroup);
const boneGroups={};
function indexGroups(groups){for(const g of groups){boneGroups[g.name]=g.uuid;indexGroups(g.children.filter(x=>typeof x==='object'));}} indexGroups(outliner);
const bbAnimations=Object.entries(animations.animations).map(([name,a])=>({uuid:crypto.randomUUID(),name,loop:a.loop?'loop':'once',override:false,length:a.animation_length,snapping:20,selected:false,anim_time_update:'',blend_weight:'',start_delay:'',loop_delay:'',animators:Object.fromEntries(Object.entries(a.bones).map(([bone,channels])=>[boneGroups[bone],{name:bone,type:'bone',keyframes:Object.entries(channels).flatMap(([channel,frames])=>Object.entries(frames).map(([time,frame])=>({channel,data_points:[{x:String(frame.vector[0]),y:String(frame.vector[1]),z:String(frame.vector[2])}],uuid:crypto.randomUUID(),time:Number(time),color:-1,interpolation:frame.lerp_mode==='catmullrom'?'catmullrom':'linear'})))}]))}));
const bb={meta:{format_version:'4.10',model_format:'geckolib_model',box_uv:true},name:'Nemesis',model_identifier:'nemesis',visible_box:[4.5,3.4,0],variable_placeholders:'',resolution:{width:128,height:128},elements,outliner,textures:[{path:'../textures/entity/nemesis.png',name:'nemesis.png',folder:'entity',namespace:'nemesis_ai',id:'0',particle:false,render_mode:'default',visible:true,mode:'bitmap'}],animations:bbAnimations,geckolib_format_version:2};

fs.writeFileSync(path.join(asset,'geo/nemesis.geo.json'),JSON.stringify(geo,null,2));
fs.writeFileSync(path.join(asset,'animations/nemesis.animation.json'),JSON.stringify(animations,null,2));
fs.writeFileSync(path.join(asset,'models/nemesis.bbmodel'),JSON.stringify(bb,null,2));
fs.copyFileSync(path.join(asset,'models/nemesis.bbmodel'),path.join(root,'nemesis.bbmodel'));
fs.copyFileSync(path.join(asset,'geo/nemesis.geo.json'),path.join(root,'nemesis.geo.json'));
fs.copyFileSync(path.join(asset,'animations/nemesis.animation.json'),path.join(root,'nemesis.animation.json'));
