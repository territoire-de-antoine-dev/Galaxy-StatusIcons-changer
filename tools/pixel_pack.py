"""Build Pixel-style packs from the Android 17 SettingsLib drawable directory and firmware dump.
Usage: python pixel_pack.py SOURCE_DIR AAPT_DUMP REPO OUTPUT_DIR V02_PROFILE
Needs resvg-py and Pillow. SOURCE_DIR also contains the optional Material SVGs listed in NOTICE.
"""
import base64, io, json, re, sys
from pathlib import Path
from functools import lru_cache
import xml.etree.ElementTree as ET
from PIL import Image,ImageDraw,ImageFont
import resvg_py
src,dump,repo,out,legacy_profile=map(Path,sys.argv[1:])
assets=repo/'app/src/main/assets'
profile=json.loads((assets/'profile.json').read_text())
old=set(json.loads(legacy_profile.read_text(encoding="utf-8-sig"))["resources"])
real={name:id for id,name in re.findall(r'^    resource (0x[0-9a-f]+) drawable/(\w+)\s*$',dump.read_text(encoding='utf-8-sig'),re.M)}
A='{http://schemas.android.com/apk/res/android}'
def vector(name):
    root=ET.parse(src/(name+'.xml')).getroot()
    xml=ET.Element('svg',xmlns='http://www.w3.org/2000/svg',viewBox='0 0 '+root.get(A+'viewportWidth')+' '+root.get(A+'viewportHeight'),width=root.get(A+'width').replace('dp',''),height=root.get(A+'height').replace('dp',''))
    def paths(parent, dest):
        for c in parent:
            if c.tag=='group':
                paths(c,ET.SubElement(dest,'g'))
            elif c.tag=='clip-path':
                clip=ET.SubElement(xml,'clipPath',id='clip')
                ET.SubElement(clip,'path',d=c.get(A+'pathData'))
                dest=ET.SubElement(dest,'g',{'clip-path':'url(#clip)'})
            elif c.tag=='path':
                ET.SubElement(dest,'path',{'d':c.get(A+'pathData'),'fill':'white','fill-opacity':c.get(A+'fillAlpha','1'),'fill-rule':'evenodd' if c.get(A+'fillType')=='evenOdd' else 'nonzero'})
            else: raise ValueError(c.tag)
    paths(root,xml)
    return ET.tostring(xml,encoding='unicode')
def mapping(key):
    k=key.removeprefix('stat_sys_')
    m=re.fullmatch(r'(?:6ewifi_signal|wifi[567]?_signal(?:_pure|_lgt|_wifi_calling)?|wifi_captive)_([0-4])',k)
    if m:
        n=[0,1,1,2,3][int(m[1])]
        return 'ic_wifi_'+str(n)+('_error' if 'captive' in k else ''),''
    m=re.fullmatch(r'signal_(?:(5level|5bar_dual[12]|dual[12]|sprint)_)?([0-5])',k)
    if m:return f'ic_mobile_{m[2]}_{5 if m[1] in ("5level","5bar_dual1","5bar_dual2","sprint") else 4}_bar',''
    if k.startswith(('data_connected_','data_disabled_','data_fully_connected_')) or re.fullmatch(r'data_\w+_na',k):
        disabled='disabled' in k
        s=re.sub(r'^data_(?:(?:fully_connected|connected|disabled)_)?(?:cross_)?','',k)
        for pattern,name in [('5g_uwb','5G UW'),('5g_uc|5guc','5G UC'),('5ga','5G-A'),('4_half_g_plus','4.5G+'),('4_half_g','4.5G'),('2g','2G'),('3g_plus','3G+')]:
            if re.match(pattern,s):return 'text:'+name, 'disabled' if disabled else ''
        for pattern,icon in [('5ge','5g_e'),('5g_plus','5g_plus'),('5g','5g'),('4g_lte_plus','4g_lte_plus'),('4glte|4g_lte','4g_lte'),('4g_plus','4g_plus'),('4g','4g'),('lte_plus','lte_plus'),('lte','lte'),('3g','3g'),('1x','1x'),('h_plus','h_plus'),('h','h'),('e','e'),('g','g')]:
            if re.match(pattern,s):return 'ic_'+icon+'_mobiledata_updated','disabled' if disabled else ('available' if 'available' in s else '')
    extras={'airplane_mode':'flight','alarm':'alarm','alarm_dim':'alarm','data_bluetooth':'bluetooth','data_bluetooth_connected':'bluetooth_connected','tether_bluetooth':'bluetooth','cast':'cast','connected_display':'cast','headset':'headset','headset_mic':'headset_mic','location':'location','ringer_silent':'volume_off','ringer_vibrate':'vibration','dnd':'dnd','do_not_disturb_mode':'dnd','data_saver':'data_saver','rotate_landscape':'screen_rotation','rotate_portrait':'screen_rotation','vpn_ic':'vpn','branded_vpn':'vpn'}
    extras.update({'ethernet':'ethernet','ethernet_fully':'ethernet','screen_record':'record','hotspot':'wifi_tethering','securewifi_ic':'securewifi','no_internet_vpn_ic':'vpn','no_internet_branded_vpn':'vpn','signal_voice_no_service_jp':'phone_disabled'})
    if k in ('no_internet_vpn_ic','no_internet_branded_vpn'):return 'vpn.svg','disabled'
    if 'roam' in k and k.startswith(('signal_roam','roaming')):return 'text:R',''
    if k=='tty_mode':return 'text:TTY',''
    # Keep the Samsung sensors-off warning intact.
    if 'null' in k and k.startswith('signal_'):return 'ic_mobile_0_'+('5' if '5' in k else '4')+'_bar','disabled'
    if 'anim_' not in k and 'no_inout' not in k and not re.search(r'att_[123]$',k):
        arrow=re.fullmatch(r'(?:data_(?:w\d+_)?|wifi_signal_(?:lgt_)?)(inout|in|out)(?:_.*)?',k)
        if arrow:return {'in':'arrow_downward.svg','out':'arrow_upward.svg','inout':'swap_vert.svg'}[arrow[1]],''
    if k in extras:return extras[k]+'.svg','dim' if k=='alarm_dim' else ''
@lru_cache(None)
def png(name,style):
    if name.startswith('text:'):
        font=ImageFont.truetype('arialbd.ttf',32)
        s=name[5:]; box=font.getbbox(s)
        im=Image.new('RGBA',(box[2]-box[0]+2,box[3]-box[1]+2))
        ImageDraw.Draw(im).text((1-box[0],1-box[1]),s,font=font,fill='white')
    else:
        svg=(src/name).read_text() if name.endswith('.svg') else vector(name)
        im=Image.open(io.BytesIO(resvg_py.svg_to_bytes(svg_string=svg,width=96))).convert('RGBA')
        a=im.getchannel('A');im=Image.new('RGBA',im.size,'white');im.putalpha(a)
    if style in ('available','dim'):im.putalpha(im.getchannel('A').point(lambda x:round(x*.6)))
    if style=='disabled':
        d=ImageDraw.Draw(im);d.line((2,1,im.width-3,im.height-2),fill=(0,0,0,0),width=6);d.line((1,2,im.width-4,im.height-1),fill='white',width=2)
    im.thumbnail((128,128)); buff=io.BytesIO();im.save(buff,format='PNG',optimize=True)
    return base64.b64encode(buff.getvalue()).decode()
icons={};codes={}
for name in sorted(real):
    if name not in old and not name.startswith(('stat_sys_wifi_signal_pure_', 'stat_sys_data_connected_', 'stat_sys_data_disabled_')):continue
    record=mapping(name)
    if record:
        icons[name]={'day':png(*record),'enabled':True}
        codes[name]=dict(id=real[name],source=record[0],variant=record[1])
profile['resources'].update({k:real[k] for k in icons})
(assets/'profile.json').write_text(json.dumps(profile,indent=2)+'\n')
pack=dict(version=1,name='Pixel · Android 17',features=dict(pixelBattery=True,chargingWave=True,satellite=True),icons=icons)
(assets/'pixel17.json').write_text(json.dumps(pack,separators=(',',':'),ensure_ascii=False),encoding='utf-8')
legacy=dict(version=1,name='Pixel · Wi-Fi et réseau',icons={k:v for k,v in icons.items() if k in old})
(out/'Pixel-WiFi-Mobile-Studio-0.2.json').write_text(json.dumps(legacy,ensure_ascii=False),encoding='utf-8')
(out/'Correspondances-Pixel-17.json').write_text(json.dumps(codes,indent=2),encoding='utf-8')
print('Pack intégré',len(icons),'pack compatible',len(legacy['icons']))
