import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import viVN from 'antd/locale/vi_VN';
import { Provider } from 'react-redux';
import { RouterProvider } from 'react-router-dom';
import './index.css';
import { store } from './app/store';
import { router } from './app/router';
import { themeConfig } from './app/theme';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <ConfigProvider theme={themeConfig} locale={viVN}>
        <RouterProvider router={router} />
      </ConfigProvider>
    </Provider>
  </React.StrictMode>,
);
