import React from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
  TouchableOpacity,
} from 'react-native';

function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <SafeAreaView style={[styles.container, isDarkMode && styles.darkBg]}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={isDarkMode ? '#1a1a2e' : '#ffffff'}
      />
      <ScrollView contentInsetAdjustmentBehavior="automatic">
        <View style={styles.header}>
          <Text style={[styles.headerText, isDarkMode && styles.lightText]}>
            🚀 React Native in G2
          </Text>
          <Text style={[styles.subtitle, isDarkMode && styles.lightSubtext]}>
            Brownfield Integration
          </Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>集成成功</Text>
          <Text style={styles.cardBody}>
            这是一个嵌入到现有 Android 项目中的 React Native 页面。
            你可以在这里构建跨平台 UI。
          </Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>下一步</Text>
          <Text style={styles.cardBody}>
            编辑 rn/App.tsx 来修改这个页面。{'\n'}
            Metro bundler 支持热重载。
          </Text>
        </View>

        <TouchableOpacity style={styles.button}>
          <Text style={styles.buttonText}>Hello from RN</Text>
        </TouchableOpacity>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  darkBg: {
    backgroundColor: '#1a1a2e',
  },
  header: {
    padding: 32,
    alignItems: 'center',
  },
  headerText: {
    fontSize: 28,
    fontWeight: '700',
    color: '#333',
  },
  lightText: {
    color: '#fff',
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    marginTop: 8,
  },
  lightSubtext: {
    color: '#aaa',
  },
  card: {
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginVertical: 8,
    padding: 20,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  cardBody: {
    fontSize: 14,
    color: '#666',
    lineHeight: 22,
  },
  button: {
    backgroundColor: '#6200ee',
    marginHorizontal: 16,
    marginVertical: 16,
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default App;
