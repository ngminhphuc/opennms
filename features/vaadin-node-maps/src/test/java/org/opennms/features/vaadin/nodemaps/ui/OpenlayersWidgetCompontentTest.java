package org.opennms.features.vaadin.nodemaps.ui;

import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.features.geocoder.Coordinates;
import org.opennms.features.geocoder.GeocoderService;
import org.opennms.netmgt.dao.AlarmDao;
import org.opennms.netmgt.dao.AssetRecordDao;
import org.opennms.netmgt.dao.NodeDao;
import org.opennms.netmgt.model.OnmsAssetRecord;
import org.opennms.netmgt.model.OnmsGeolocation;
import org.opennms.netmgt.model.OnmsNode;

import com.vaadin.terminal.PaintTarget;

public class OpenlayersWidgetCompontentTest {
    private NodeDao m_nodeDao;
    private AssetRecordDao m_assetDao;
    private AlarmDao m_alarmDao;
    private GeocoderService m_geocoder;
    private OpenlayersWidgetComponent m_component;

    @Before
    public void setUp() {
        m_nodeDao = EasyMock.createMock(NodeDao.class);
        m_assetDao = EasyMock.createMock(AssetRecordDao.class);
        m_alarmDao = EasyMock.createMock(AlarmDao.class);
        m_geocoder = EasyMock.createMock(GeocoderService.class);
        m_component = new OpenlayersWidgetComponent(m_nodeDao, m_assetDao, m_alarmDao, m_geocoder);
    }

    @Test
    @Ignore
    public void testGeolocation() throws Exception {
        final OnmsNode node = new OnmsNode();
        final OnmsAssetRecord asset = new OnmsAssetRecord();
        final OnmsGeolocation geo = new OnmsGeolocation();
        
        node.setId(1);
        node.setAssetRecord(asset);
        asset.setGeolocation(geo);

        geo.setAddress1("220 Chatham Business Dr.");
        geo.setCity("Pittsboro");
        geo.setState("NC");
        geo.setZip("27312");
        
        assertEquals("220 Chatham Business Dr., Pittsboro, NC 27312", geo.asAddressString());

        EasyMock.expect(m_geocoder.getCoordinates(geo.asAddressString())).andReturn(new Coordinates(1.0f, -1.0f)).times(1);
        final PaintTarget target = EasyMock.createMock(PaintTarget.class);

        m_assetDao.saveOrUpdate(EasyMock.isA(OnmsAssetRecord.class));

        target.startTag(EasyMock.eq("1"));
        target.addAttribute(EasyMock.eq("latitude"), EasyMock.eq("1.0"));
        target.addAttribute(EasyMock.eq("longitude"), EasyMock.eq("-1.0"));
        target.endTag(EasyMock.eq("1"));
        
        EasyMock.replay(m_nodeDao, m_assetDao, m_geocoder, target);

        m_component.addNode(target, node);
        
        EasyMock.verify(m_nodeDao, m_assetDao, m_geocoder, target);
    }
}
