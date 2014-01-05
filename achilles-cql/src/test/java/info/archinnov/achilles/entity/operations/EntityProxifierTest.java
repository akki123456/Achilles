package info.archinnov.achilles.entity.operations;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import info.archinnov.achilles.context.PersistenceContext;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.proxy.EntityInterceptor;
import info.archinnov.achilles.test.builders.CompleteBeanTestBuilder;
import info.archinnov.achilles.test.mapping.entity.CompleteBean;
import info.archinnov.achilles.test.mapping.entity.UserBean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.NoOp;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityProxifierTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@InjectMocks
	private EntityProxifier proxifier;

	@Mock
	private EntityInterceptor<CompleteBean> interceptor;

	@Mock
	private PersistenceContext context;

	@Mock
	private EntityMeta entityMeta;

	@Mock
	private PropertyMeta idMeta;

	@Test
	public void should_derive_base_class_from_transient() throws Exception {
		assertThat(proxifier.<CompleteBean> deriveBaseClass(new CompleteBean())).isEqualTo(CompleteBean.class);
	}

	@Test
	public void should_derive_base_class() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().id(1L).buid();
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(entity.getClass());
		enhancer.setCallback(interceptor);

		when(interceptor.getTarget()).thenReturn(entity);

		CompleteBean proxy = (CompleteBean) enhancer.create();
		assertThat(proxifier.<CompleteBean> deriveBaseClass(proxy)).isEqualTo(CompleteBean.class);
	}

	@Test
	public void should_build_proxy_with_eager_fields_loaded() throws Exception {

		long primaryKey = RandomUtils.nextLong();

		CompleteBean entity = CompleteBeanTestBuilder.builder().id(primaryKey).name("name").buid();

		proxifier = spy(proxifier);

		doReturn(interceptor).when(proxifier).buildInterceptor(eq(context), eq(entity), anySetOf(Method.class));
		when(entityMeta.getIdMeta()).thenReturn(idMeta);

		CompleteBean proxy = proxifier.buildProxyWithEagerFieldsLoaded(entity, context);

		assertThat(proxy).isNotNull();
		assertThat(proxy).isInstanceOf(Factory.class);
		Factory factory = (Factory) proxy;

		assertThat(factory.getCallbacks()).hasSize(1);
		assertThat(factory.getCallback(0)).isInstanceOf(EntityInterceptor.class);
	}

	@Test
	public void should_build_null_proxy() throws Exception {
		assertThat(proxifier.buildProxyWithEagerFieldsLoaded(null, context)).isNull();
	}

	@Test
	public void should_get_real_object_from_proxy() throws Exception {
		UserBean realObject = new UserBean();
		when(interceptor.getTarget()).thenReturn(realObject);

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(UserBean.class);
		enhancer.setCallback(interceptor);
		UserBean proxy = (UserBean) enhancer.create();

		UserBean actual = proxifier.getRealObject(proxy);

		assertThat(actual).isSameAs(realObject);
	}

	@Test
	public void should_return_object_when_get_real_object_called_on_non_proxified_entity() throws Exception {
		UserBean realObject = new UserBean();

		UserBean actual = proxifier.getRealObject(realObject);
		assertThat(actual).isSameAs(realObject);

	}

	@Test
	public void should_proxy_true() throws Exception {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(CompleteBean.class);
		enhancer.setCallback(NoOp.INSTANCE);

		CompleteBean proxy = (CompleteBean) enhancer.create();

		assertThat(proxifier.isProxy(proxy)).isTrue();
	}

	@Test
	public void should_proxy_false() throws Exception {
		CompleteBean bean = CompleteBeanTestBuilder.builder().id(1L).buid();
		assertThat(proxifier.isProxy(bean)).isFalse();
	}

	@Test
	public void should_get_interceptor_from_proxy() throws Exception {

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(CompleteBean.class);
		enhancer.setCallback(interceptor);
		CompleteBean proxy = (CompleteBean) enhancer.create();

		EntityInterceptor<CompleteBean> actual = proxifier.getInterceptor(proxy);

		assertThat(actual).isSameAs(interceptor);
	}

	@Test
	public void should_ensure_proxy() throws Exception {
		proxifier.ensureProxy(realProxy);
	}

	@Test
	public void should_exception_when_not_proxy() throws Exception {
		CompleteBean proxy = new CompleteBean();

		exception.expect(IllegalStateException.class);
		exception.expectMessage("The entity '" + proxy + "' is not in 'managed' state.");
		proxifier.ensureProxy(proxy);
	}

    @Test
    public void should_ensure_not_proxy() throws Exception {
        proxifier.ensureNotProxy(new CompleteBean());
    }

    @Test
    public void should_exception_when_proxy() throws Exception {

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Then entity is already in 'managed' state");
        proxifier.ensureNotProxy(realProxy);
    }

	@Test
	public void should_return_null_when_unproxying_null() throws Exception {
		assertThat(proxifier.removeProxy((Object) null)).isNull();
	}

	@Test
	public void should_return_same_entity_when_calling_unproxy_on_non_proxified_entity() throws Exception {
		CompleteBean realObject = new CompleteBean();

		CompleteBean actual = proxifier.removeProxy(realObject);

		assertThat(actual).isSameAs(realObject);
	}

	@Test
	public void should_unproxy_entity() throws Exception {
		when(interceptor.getTarget()).thenReturn(realProxy);

		Factory actual = proxifier.removeProxy(realProxy);

		assertThat(actual).isSameAs(realProxy);
	}

	@Test
	public void should_unproxy_real_entryset() throws Exception {
		Map<Integer, CompleteBean> map = new HashMap<Integer, CompleteBean>();
		CompleteBean completeBean = new CompleteBean();
		map.put(1, completeBean);
		Map.Entry<Integer, CompleteBean> entry = map.entrySet().iterator().next();

		when(proxifier.isProxy(completeBean)).thenReturn(false);

		Map.Entry<Integer, CompleteBean> actual = proxifier.removeProxy(entry);
		assertThat(actual).isSameAs(entry);
		assertThat(actual.getValue()).isSameAs(completeBean);
	}

	@Test
	public void should_unproxy_entryset_containing_proxy() throws Exception {
		Map<Integer, Factory> map = new HashMap<Integer, Factory>();
		map.put(1, realProxy);
		Map.Entry<Integer, Factory> entry = map.entrySet().iterator().next();

		when(interceptor.getTarget()).thenReturn(realProxy);

		Map.Entry<Integer, Factory> actual = proxifier.removeProxy(entry);
		assertThat(actual).isSameAs(entry);
		assertThat(actual.getValue()).isSameAs(realProxy);
	}

	@Test
	public void should_unproxy_collection_of_entities() throws Exception {
		Collection<Factory> proxies = new ArrayList<Factory>();
		proxies.add(realProxy);

		when(interceptor.getTarget()).thenReturn(realProxy);

		Collection<Factory> actual = proxifier.removeProxy(proxies);

		assertThat(actual).containsExactly(realProxy);
	}

	@Test
	public void should_unproxy_list_of_entities() throws Exception {
		List<Factory> proxies = new ArrayList<Factory>();
		proxies.add(realProxy);

		when(interceptor.getTarget()).thenReturn(realProxy);

		Collection<Factory> actual = proxifier.removeProxy(proxies);

		assertThat(actual).containsExactly(realProxy);
	}

	@Test
	public void should_unproxy_set_of_entities() throws Exception {
		Set<Factory> proxies = new HashSet<Factory>();
		proxies.add(realProxy);

		when(interceptor.getTarget()).thenReturn(realProxy);

		Collection<Factory> actual = proxifier.removeProxy(proxies);

		assertThat(actual).containsExactly(realProxy);
	}

	private Factory realProxy = new Factory() {

		@Override
		public Object newInstance(Callback callback) {
			return null;
		}

		@Override
		public Object newInstance(Callback[] callbacks) {
			return null;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Object newInstance(Class[] types, Object[] args, Callback[] callbacks) {
			return null;
		}

		@Override
		public Callback getCallback(int index) {
			return interceptor;
		}

		@Override
		public void setCallback(int index, Callback callback) {

		}

		@Override
		public void setCallbacks(Callback[] callbacks) {

		}

		@Override
		public Callback[] getCallbacks() {
			return new Callback[] { interceptor };
		}

	};
}
